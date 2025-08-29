package cloud.contoterzi.aws.claude;

import cloud.contoterzi.aws.common.AbstractBedrockDriver;
import cloud.contoterzi.helpdesk.core.llm.InvalidRequestException;
import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import software.amazon.awssdk.regions.Region;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thin delegate that calls AWS Bedrock Anthropic model.
 * - No retries, no timing, no logging: these are handled by the adapter/abstract client.
 * - Returns only the raw text answer extracted from the response.
 */
public class ClaudeClientBedrock extends AbstractBedrockDriver {
    // Static client that reuses the connections
    private static BedrockRuntimeClient staticClient;
    private static final Object CLIENT_LOCK = new Object();

    private BedrockRuntimeClient client;
    private String modelId;
    private int maxTokens;
    private String anthropicVersion; // e.g., "bedrock-2023-05-31"
    private double temperature;
    private String systemPrompt = ""; // Will use prompts.preamble from config
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor required for SPI loading.
     * Configuration will be provided via init() method.
     */
    public ClaudeClientBedrock() {
        // Will be initialized in init()
    }


    /**
     * Performs a single "messages" call to Claude via Bedrock and returns the first text block.
     * This method DOES NOT implement retries and DOES NOT measure time.
     * Exceptions are propagated to be handled by upper layers.
     */
    @Override
    protected LlmResponse callTheLLM(LlmRequest request) throws LlmException {
        if (client == null) {
            throw new IllegalStateException("ClaudeClientBedrock not initialized. Call init() first.");
        }
        
        Map<String, Object> payload = getStringObjectMap(request);

        try {
            String jsonPayload = mapper.writeValueAsString(payload);

            InvokeModelRequest req = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            // HERE IT CALLS AWS
            InvokeModelResponse resp = client.invokeModel(req);
            // Return the raw JSON response
            return new LlmResponse(resp.body().asUtf8String());
        }
        catch (JsonProcessingException ex) {
            throw new InvalidRequestException("Problem converting the request to JSON", ex);
        }
    }

    private Map<String, Object> getStringObjectMap(LlmRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("Request and its prompt cannot be null or empty");
        }

        final String prompt = request.getPrompt();

        // Build the payload - use system parameter for Claude 3 if systemPrompt is available
        if (!systemPrompt.isEmpty()) {
            return Map.of(
                "anthropic_version", anthropicVersion,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "system", systemPrompt,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of(
                                "type", "text",
                                "text", prompt.trim()
                            )
                        )
                    )
                )
            );
        } else {
            // No system prompt - use standard message format
            return Map.of(
                "anthropic_version", anthropicVersion,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of(
                                "type", "text",
                                "text", prompt.trim()
                            )
                        )
                    )
                )
            );
        }
    }

    @Override
    public String id() {
        return "claude";
    }

    @Override
    public void init(AppConfig config) {
        LlmConfig llmConfig = config.getLlm();
        this.modelId = llmConfig.getModelId();
        this.maxTokens = llmConfig.getMaxTokens();
        this.anthropicVersion = llmConfig.getLlmVersion();
        this.temperature = llmConfig.getTemperature();
        
        // Initialize prompts configuration
        if (llmConfig.getPrompts() != null) {
            this.systemPrompt = llmConfig.getPrompts().getPreamble() != null 
                ? llmConfig.getPrompts().getPreamble() : "";
        }

        synchronized (CLIENT_LOCK) {
            if (staticClient == null) {
                String region = llmConfig.getRegion();
                if (region == null || region.trim().isEmpty()) {
                    throw new IllegalArgumentException("AWS region cannot be null or empty");
                }

                staticClient = BedrockRuntimeClient.builder()
                        .region(Region.of(llmConfig.getRegion()))
                        // connection optimization
                        .overrideConfiguration(builder -> builder
                                .apiCallTimeout(Duration.ofSeconds(30))
                                .apiCallAttemptTimeout(Duration.ofSeconds(15))
                        ).build();

            }
            // Create Bedrock client with region from config
            this.client = staticClient;
        }
    }
}