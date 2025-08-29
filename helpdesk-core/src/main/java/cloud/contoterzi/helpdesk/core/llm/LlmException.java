package cloud.contoterzi.helpdesk.core.llm;

/**
 * Base class for all the exceptions thrown by LLM.
 */
public abstract class LlmException extends Exception {

    public static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";

    protected LlmException(String message, Throwable cause) {
        super(normalizeMessage(message, cause), cause);
    }

    protected LlmException(String message) {
        this(message, null);
    }

    protected LlmException(Throwable cause) {
        this(null, cause);
    }

    private static String normalizeMessage(String message, Throwable cause) {
        if (message != null && !message.isBlank())
            return message;
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && !causeMessage.isBlank())
                return causeMessage;

            return cause.getClass().getSimpleName();
        }
        return UNKNOWN_ERROR_MESSAGE;
    }

    /**
     * Indicates whether the error is retryable.
     */
    public abstract boolean isRetryable();

    /**
     * Indicates whether the error is not retryable.
     * @return true if the error is not retryable, false otherwise.
     */
    public boolean isNotRetryable() {
        return !isRetryable();
    }
}
