package cloud.contoterzi.helpdesk.core.llm;

/**
 * Client-side input error (e.g., invalid parameters or prompt).
 * Provider-agnostic. Non-retryable by default.
 */
public class InvalidRequestException extends LlmException {

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean isRetryable() {
        // Malformed requests are not retryable.
        return false;
    }
}
