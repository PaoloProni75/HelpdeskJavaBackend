package cloud.contoterzi.aws.common;

import cloud.contoterzi.helpdesk.core.llm.*;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import software.amazon.awssdk.core.exception.SdkServiceException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.*;

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

    private static final List<String> TIMEOUT_PATTERNS = List.of(
            "timed out", "timeout", "read timed out", "connection timed out"
    );

    /**
     * Abstract method that concrete implementations must override to handle the specific LLM API.
     * @param request the LLM request containing the prompt and other parameters
     * @return the LLM response with the answer and metadata
     * @throws LlmException if there is an error calling the LLM
     */
    protected abstract LlmResponse callTheLLM(LlmRequest request) throws LlmException;

    @Override
    protected LlmException wrap(Throwable t) {
        return new ProviderException(t);
    }

    private boolean isTimeout(Throwable t) {
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

}