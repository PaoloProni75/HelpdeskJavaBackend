package cloud.contoterzi.helpdesk.core.llm;

/**
 * Timeout exception for the connection or the response.
 * This error indicates that the LLM API did not respond within the expected timeframe.
 * This is usually due to a slow or unresponsive LLM server.
 * The client should retry the request.
 */
public class TimeoutException extends LlmException {

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
