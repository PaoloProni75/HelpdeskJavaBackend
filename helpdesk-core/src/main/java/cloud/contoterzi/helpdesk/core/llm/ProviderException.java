package cloud.contoterzi.helpdesk.core.llm;

/**
 * Generic exception for LLM providers (e.g., upstream service failures).
 * Provider-agnostic: carries optional context like statusCode and retryAfterMs,
 * without enforcing retry policies.
 */
public class ProviderException extends LlmException {

    public static final int  UNKNOWN_STATUS         = -1;
    public static final long UNKNOWN_RETRY_AFTER_MS = -1L;

    private final int  statusCode;
    private final long retryAfterMs;


    /**
     * Constructs a new ProviderException with the specified message, cause,
     * status code, and retry-after duration.
     *
     * @param message The detailed message for the exception.
     * @param cause The underlying cause of the exception, or null if none.
     * @param statusCode The HTTP-like status code associated with the error, or -1 if unknown.
     * @param retryAfterMs The suggested retry-after duration in milliseconds, or -1 if unknown.
     */
    public ProviderException(String message, Throwable cause, int statusCode, long retryAfterMs) {
        super(message, cause);
        this.statusCode   = statusCode;
        this.retryAfterMs = retryAfterMs;
    }

    // Convenience constructors
    public ProviderException(String message) {
        this(message, null, UNKNOWN_STATUS, UNKNOWN_RETRY_AFTER_MS);
    }

    public ProviderException(String message, int statusCode) {
        this(message, null, statusCode, UNKNOWN_RETRY_AFTER_MS);
    }

    public ProviderException(String message, Throwable cause) {
        this(message, cause, UNKNOWN_STATUS, UNKNOWN_RETRY_AFTER_MS);
    }

    public ProviderException(Throwable cause) {
        this(null, cause, UNKNOWN_STATUS, UNKNOWN_RETRY_AFTER_MS);
    }

    public ProviderException(Throwable cause, int statusCode, long retryAfterMs) {
        this(null, cause, statusCode, retryAfterMs);
    }

    public int getStatusCode()    { return statusCode; }
    public long getRetryAfterMs() { return retryAfterMs; }

    @Override
    public boolean isRetryable() {
        return true; // core stays neutral; driver decides when to throw
    }
}