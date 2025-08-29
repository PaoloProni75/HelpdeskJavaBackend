package cloud.contoterzi.helpdesk.core.llm;

/**
 * Authentication/authorization failure (e.g., missing/invalid credentials).
 * Provider-agnostic. Non-retryable by default.
 */
public class AuthException extends LlmException {
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthException(String message) {
        super(message);
    }

    public AuthException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean isRetryable() {
        // When the credentials are wrong, it makes no sense to retry.
        return false;
    }
}
