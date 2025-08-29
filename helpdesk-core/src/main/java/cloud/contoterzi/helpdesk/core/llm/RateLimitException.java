package cloud.contoterzi.helpdesk.core.llm;

/**
 * Exception thrown when a rate limit has been exceeded.
 * This error indicates that too many requests have been made to the LLM API
 * within a specific timeframe, and the client must wait before retrying.
 */
public class RateLimitException extends LlmException {

    public static final long UNKNOWN_RETRY_AFTER_MS = -1L;

    private final long retryAfterMs; // -1 if unknown

    public RateLimitException(String message, Throwable cause, long retryAfterMs) {
        super(message, cause);
        this.retryAfterMs = retryAfterMs;
    }

    public RateLimitException(String message, long retryAfterMs) {
        this(message, null, retryAfterMs);
    }

    public RateLimitException(Throwable cause, long retryAfterMs) {
        this(null, cause, retryAfterMs);
    }

    public RateLimitException(String message) {
        this(message, null, UNKNOWN_RETRY_AFTER_MS);
    }

    public RateLimitException(Throwable cause) {
        this(null, cause, UNKNOWN_RETRY_AFTER_MS);
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
