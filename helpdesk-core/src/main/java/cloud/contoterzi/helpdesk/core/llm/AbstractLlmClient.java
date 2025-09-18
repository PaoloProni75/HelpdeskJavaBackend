package cloud.contoterzi.helpdesk.core.llm;

import cloud.contoterzi.helpdesk.core.handler.HandlerConstants;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;

import java.io.InterruptedIOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.InterruptedByTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import cloud.contoterzi.helpdesk.core.spi.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for LLM clients.
 * This class provides:
 *  <ul>
 *      <li>Standard timing</li>
 *      <li>Minimum sanitization of Input / Output</li>
 *      <li>Optional retry based on LlmException#isNotRetryable()</li>
 *      <li>Overridable hooks for error mapping and provider-specific backoff</li>
 *  </ul>
 *
 *  <p>
 *      The subclasses must implement the invokeProvider(...) method and
 *      override the wrap(...) and / or the backoff(...) methods.
 *  </p>
 */
public abstract class AbstractLlmClient implements LlmClient {
    protected static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static final int CAUSE_SCAN_MAX_DEPTH = 10;

    private static final String UNKNOWN_ERROR = "Unknown error";
    private static final String RATE_LIMIT_EXCEPTION_MESSAGE = "Rate limit exceeded (429)";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request (400): %s";
    private static final String SERVICE_TIMEOUT_MESSAGE = "Service timeout (status %d): %s";
    private static final String AUTH_ERROR_MESSAGE_FORMAT = "Authorization error";
    private static final String PROVIDER_EXCEPTION_FORMAT = "ProviderException (status %d): %s";
    private static final String CLIENT_NETWORK_TIMEOUT_FORMAT = "Client/network timeout";
    private static final String ERROR_TIMEOUT_MESSAGE_FORMAT = "Generic timeout detected via message/causes: %s";
    private static final String GENERIC_PROVIDER_ERROR_FORMAT = "Generic provider error";
    private static final long RETRY_AFTER_UNKNOWN_MS = -1L;

    /**
     * Main invocation point for the client.
     * Uses retry logic with sensible defaults for all LLM providers.
     * @param request  The request to the LLM.
     * @return The LLM response.
     * @throws LlmException If there is an error.
     */
    @Override
    public final LlmResponse ask(LlmRequest request) throws LlmException {
        return askWithRetry(request, 3, Duration.ofMillis(500));
    }

    /**
     * Direct invocation without retry logic.
     * Use this for testing or when you want to handle retries manually.
     * @param request  The request to the LLM.
     * @return The LLM response.
     * @throws LlmException If there is an error.
     */
    public LlmResponse askDirect(LlmRequest request) throws LlmException {
        Objects.requireNonNull(request, "req must not be null in AbstractLlmClient.askDirect");

        final long t0 = System.currentTimeMillis();
        try {
            // HERE IS THE DELEGATION TO THE SPECIFIC DRIVER IMPLEMENTATION
            LlmResponse answer = invokeProvider(request);
            long ms = System.currentTimeMillis() - t0;
            answer.setTimeMs(ms);
            return answer;
        } catch (LlmException e) {
            throw e; // Already mapped in the driver
        } catch (Throwable t) {
            // Mapping of fallback in the core: the driver can do an override with a more precise logic.
            throw wrap(t);
        }
    }

    /**
     * Convenience method for LLM clients that don't need to specify the prompt.
     * @param prompt The prompt to ask.
     * @return The LLM response.
     * @throws LlmException If there is an error.
     */

    public LlmResponse ask(String prompt) throws LlmException {
        return ask(new LlmRequest(prompt));
    }

    /**
     * Fallback mapping from Throwable to LlmException.
     * The subclass can override this method to provide a more precise mapping.
     * @param t The Throwable.
     * @return The LlmException.
     */
    protected LlmException wrap(Throwable t) {
        String msg = (t != null && t.getMessage() != null) ? t.getMessage() : "LLM provider error";
        return new ProviderException(msg, t);
    }

    /**
     * Executes the askDirect(...) method with retry/backoff for retryable errors.
     * The decision of retrying derives from e.isNotRetryable() only.
     * The backoff is calculated by the backoffFor(...) method, and it is overridable by the driver.
     */
    public LlmResponse askWithRetry(LlmRequest req, int maxAttempts, Duration baseBackoff) throws LlmException {
        int attempts = Math.max(1, maxAttempts);
        LlmException last = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return askDirect(req);
            } catch (LlmException ex) {
                last = ex;
                if (ex.isNotRetryable() || attempt == attempts) {
                    throw ex; // It is not retryable of the trials are finished
                }
                long sleepMs = Math.max(0, backoffFor(ex, attempt, baseBackoff));
                sleepQuietly(sleepMs);
            }
        }
        // Theoretically here the execution should never get here.
        throw last;
    }

    /**
     * Determines the backoff delay before the next retry attempt for a given LLM exception.
     * <p>
     * By default, this method ignores the exception details and delegates to
     * {@link #jitteredBackoff(Duration, int)} with the provided base backoff and attempt count.
     * <p>
     * Drivers can override this method to:
     * <ul>
     *   <li>Adjust the backoff based on the type of exception (e.g., rate limiting vs. network error).</li>
     *   <li>Respect {@code Retry-After} headers or provider-specific wait times for {@link RateLimitException}.</li>
     *   <li>Use different backoff strategies for transient vs. non-transient errors.</li>
     * </ul>
     * <p>
     * This method is typically called by retry loops in concrete LLM client implementations.
     *
     * @param ex         the {@link LlmException} that caused the retry
     * @param attempt    the current retry attempt (1-based)
     * @param baseBackoff the base backoff duration
     * @return the computed backoff delay in milliseconds
     */
    protected long backoffFor(LlmException ex, int attempt, Duration baseBackoff) {
        // If the provider returned an explicit retry delay, honor it
        if (ex instanceof RateLimitException rle && rle.getRetryAfterMs() > 0) {
            return rle.getRetryAfterMs();
        }

        // Otherwise, use the default exponential backoff with jitter
        return jitteredBackoff(baseBackoff, attempt);
    }

    /**
     * Computes an exponential backoff with jitter to avoid synchronized retries.
     * <p>
     * The algorithm works as follows:
     * <ol>
     *   <li>Convert the base duration to milliseconds. If {@code base} is {@code null}, use 0.</li>
     *   <li>Multiply the base by {@code 2^(attempt-1)} to get an exponential delay.</li>
     *   <li>Apply a safety cap of 5000 ms to prevent excessively long waits.</li>
     *   <li>Return a random value between 0 and {@code cap} (inclusive) to introduce jitter,
     *       which reduces the risk of retry storms if multiple clients fail at the same time.</li>
     * </ol>
     * <p>
     * Example with base = 200 ms:
     * <pre>
     * Attempt 1 -> cap = 200   -> wait random [0..200] ms
     * Attempt 2 -> cap = 400   -> wait random [0..400] ms
     * Attempt 3 -> cap = 800   -> wait random [0..800] ms
     * ...
     * Attempt 6 -> cap = 5000  -> wait random [0..5000] ms (due to cap)
     * </pre>
     *
     * @param base    the base delay duration
     * @param attempt the current retry attempt (1-based)
     * @return a randomized delay in milliseconds
     */
    protected long jitteredBackoff(Duration base, int attempt) {
        long baseMs = Math.max(0, base == null ? 0 : base.toMillis());
        long max = (long) (baseMs * Math.pow(2, Math.max(0, attempt - 1))); // expo
        long cap = Math.min(max, 5_000L); // safety cap (5 seconds)
        return ThreadLocalRandom.current().nextLong(cap + 1); // [0..cap]
    }

    private void sleepQuietly(long ms) throws LlmException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Retry interrupted", ie);
        }
    }

    @SafeVarargs
    public static boolean hasCause(Throwable t, Class<? extends Throwable>... types) {
        if (t == null || types == null || types.length == 0) return false;

        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return Stream.iterate(t, Objects::nonNull, Throwable::getCause)
                .limit(CAUSE_SCAN_MAX_DEPTH)               // belt
                .takeWhile(seen::add)                      // suspenders: stop if cycle detected
                .anyMatch(cur -> Arrays.stream(types).anyMatch(c -> c.isInstance(cur)));
    }

    protected static String safeLower(String s) {
        return (s == null ? "" : s).toLowerCase(Locale.ROOT);
    }

    protected String message(Throwable t) {
        return (t != null && t.getMessage() != null)
                ? t.getMessage()
                : (t != null ? t.getClass().getSimpleName() : HandlerConstants.UNKNOWN_ERROR);
    }

    private boolean isRateLimitException(Throwable t) {
        String msg = safeLower(message(t));
        return msg.contains("rate limit") ||
                msg.contains("429") ||
                msg.contains("too many requests") ||
                msg.contains("quota exceeded");
    }

    private boolean isInvalidRequestException(Throwable t) {
        String msg = safeLower(message(t));
        return msg.contains("400") ||
                msg.contains("bad request") ||
                msg.contains("invalid request") ||
                msg.contains("validation");
    }

    private boolean isAuthException(Throwable t) {
        String msg = safeLower(message(t));
        return msg.contains("401") ||
                msg.contains("403") ||
                msg.contains("unauthorized") ||
                msg.contains("forbidden") ||
                msg.contains("authentication") ||
                msg.contains("api key");
    }

    /**
     * Checks if the passed throwable (exception) is related to a timeout
     * @param t Throwable to be checked
     * @return true if the passed exception is about a reached timeout during the call to the llm
     */
    protected abstract boolean isTimeoutException(Throwable t);

    /**
     * Template method: the subclass talks to the LLM provider and returns the textual response.
     *
     * @param req The request to the LLM.
     * @return The textual response from the LLM provider.
     * @throws LlmException (Auth/RateLimit/Timeout/Provider/InvalidRequest) according to the case
     */
    protected LlmResponse invokeProvider(LlmRequest req) throws LlmException {
        try {
            return callTheLLM(req);
        } catch (Exception ex) {
            // Handle HTTP-like exceptions based on status codes or message patterns
            if (isRateLimitException(ex)) {
                LOGGER.debug(RATE_LIMIT_EXCEPTION_MESSAGE, ex);
                throw new RateLimitException(message(ex), RETRY_AFTER_UNKNOWN_MS);
            } else if (isInvalidRequestException(ex)) {
                LOGGER.debug(INVALID_REQUEST_MESSAGE, ex);
                throw new InvalidRequestException(ex);
            } else if (isTimeoutException(ex)) {
                LOGGER.debug(CLIENT_NETWORK_TIMEOUT_FORMAT, ex);
                throw new TimeoutException(ex);
            } else if (isAuthException(ex)) {
                LOGGER.debug(AUTH_ERROR_MESSAGE_FORMAT, ex);
                throw new AuthException(ex);
            } else {
                LOGGER.debug(GENERIC_PROVIDER_ERROR_FORMAT, ex);
                throw new ProviderException(ex);
            }
        }
    }

    /**
     * Abstract method that concrete implementations must override to handle the specific LLM API.
     * @param request the LLM request containing the prompt and other parameters
     * @return the LLM response with the answer and metadata
     * @throws LlmException if there is an error calling the LLM
     */
    protected abstract LlmResponse callTheLLM(LlmRequest request) throws LlmException;
}