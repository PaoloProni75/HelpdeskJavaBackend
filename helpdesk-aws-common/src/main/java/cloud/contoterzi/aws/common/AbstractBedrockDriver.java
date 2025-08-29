package cloud.contoterzi.aws.common;

import cloud.contoterzi.helpdesk.core.llm.*;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.InterruptedByTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Abstract base class for all AWS Bedrock LLM adapters.
 * Provides common exception handling and SPI integration logic.
 * Concrete implementations need to provide the raw client delegate and initialization logic.
 */
public abstract class AbstractBedrockDriver extends AbstractLlmClient  {
    
    // HTTP status codes
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_REQUEST_TIMEOUT = 408;
    private static final int HTTP_GATEWAY_TIMEOUT = 504;

    private static final long RETRY_AFTER_UNKNOWN_MS = -1L;
    private static final int CAUSE_SCAN_MAX_DEPTH = 10;

    private static final List<String> TIMEOUT_PATTERNS = List.of(
            "timed out", "timeout", "read timed out", "connection timed out"
    );

    // Error message constants
    public static final String UNKNOWN_ERROR = "Unknown error";
    public static final String THROTTLING_EXCEPTION_MESSAGE = "ThrottlingException (rate limit): %s";
    public static final String VALIDATION_EXCEPTION_MESSAGE = "ValidationException: %s";
    public static final String SERVICE_TIMEOUT_MESSAGE = "Service timeout (status %d): %s";
    public static final String AUTH_ERROR_MESSAGE_FORMAT = "Auth error (status %d): %s";
    public static final String PROVIDER_EXCEPTION_FORMAT = "ProviderException (status %d): %s";
    public static final String CLIENT_NETWORK_TIMEOUT_FORMAT = "Client/network timeout: %s";
    public static final String ERROR_TIMEOUT_MESSAGE_FORMAT = "Generic timeout detected via message/causes: %s";
    public static final String GENERIC_PROVIDER_ERROR_FORMAT = "Generic provider error: %s";

    private static final Logger LOGGER = Logger.getLogger(AbstractBedrockDriver.class.getName());

    /**
     * Abstract method that concrete implementations must override to handle the specific LLM API.
     * @param request the LLM request containing the prompt and other parameters
     * @return the LLM response with the answer and metadata
     * @throws LlmException if there is an error calling the LLM
     */
    protected abstract LlmResponse callTheLLM(LlmRequest request) throws LlmException;

    @Override
    protected LlmResponse invokeProvider(LlmRequest req) throws LlmException {
        try {
            return callTheLLM(req);
        } catch (ThrottlingException ex) {
            // 429/Throttling -> retryable
            LOGGER.fine(() -> THROTTLING_EXCEPTION_MESSAGE.formatted(message(ex)));
            throw new RateLimitException(message(ex), RETRY_AFTER_UNKNOWN_MS);
        } catch (ValidationException ex) {
            // Invalid request: non-retryable
            LOGGER.fine(() -> VALIDATION_EXCEPTION_MESSAGE.formatted(message(ex)));
            throw new InvalidRequestException(ex);
        } catch (SdkServiceException ex) {
            // Service-side timeouts (408/504)?
            LOGGER.fine(() -> SERVICE_TIMEOUT_MESSAGE.formatted(ex.statusCode(), message(ex)));
            if (isTimeout(ex)) {
                throw new TimeoutException(ex);
            }
            int sc = ex.statusCode();
            if (sc == HTTP_UNAUTHORIZED || sc == HTTP_FORBIDDEN) {
                LOGGER.warning(() -> AUTH_ERROR_MESSAGE_FORMAT.formatted(sc, message(ex)));
                throw new AuthException(ex);
            }

            LOGGER.fine(() -> PROVIDER_EXCEPTION_FORMAT.formatted(sc, message(ex)));
            throw new ProviderException(ex, sc, RETRY_AFTER_UNKNOWN_MS);

        } catch (SdkClientException ex) {
            // Network/client timeout?
            if (isTimeout(ex)) {
                LOGGER.fine(() -> CLIENT_NETWORK_TIMEOUT_FORMAT.formatted(message(ex)));
                throw new TimeoutException(ex);
            }
            throw new ProviderException(ex);

        } catch (Exception ex) {
            // Final fallback
            if (isTimeout(ex)) {
                LOGGER.fine(() -> ERROR_TIMEOUT_MESSAGE_FORMAT.formatted(message(ex)));
                throw new TimeoutException(ex);
            }
            LOGGER.fine(() -> GENERIC_PROVIDER_ERROR_FORMAT.formatted(message(ex)));
            throw new ProviderException(ex);
        }
    }

    @Override
    protected LlmException wrap(Throwable t) {
        return new ProviderException(t);
    }

    private static String message(Throwable t) {
        return (t != null && t.getMessage() != null)
                ? t.getMessage()
                : (t != null ? t.getClass().getSimpleName() : UNKNOWN_ERROR);
    }

    private static boolean isTimeout(Throwable t) {
        // 1) Service status codes that are timeout-like
        if (t instanceof SdkServiceException sse) {
            int sc = sse.statusCode();
            if (sc == HTTP_REQUEST_TIMEOUT || sc == HTTP_GATEWAY_TIMEOUT) return true;
        }
        // 2) Classical timeout types in the cause chain
        if (hasCause(t,
                SocketTimeoutException.class,
                HttpTimeoutException.class,
                InterruptedByTimeoutException.class,
                InterruptedIOException.class)) {
            return true;
        }
        // 3) Fallback on message patterns (null-safe, neutral locale)
        final String msg = safeLower(message(t));
        return TIMEOUT_PATTERNS.stream().anyMatch(p -> msg.contains(p.toLowerCase(Locale.ROOT)));
    }

    @SafeVarargs
    private static boolean hasCause(Throwable t, Class<? extends Throwable>... types) {
        if (t == null || types == null || types.length == 0) return false;

        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return Stream.iterate(t, Objects::nonNull, Throwable::getCause)
                .limit(CAUSE_SCAN_MAX_DEPTH)               // belt
                .takeWhile(seen::add)                      // suspenders: stop if cycle detected
                .anyMatch(cur -> Arrays.stream(types).anyMatch(c -> c.isInstance(cur)));
    }

    private static String safeLower(String s) {
        return (s == null ? "" : s).toLowerCase(Locale.ROOT);
    }

}