package cloud.contoterzi.aws.nova;

import cloud.contoterzi.aws.common.AbstractBedrockDriver;
import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cloud.contoterzi.helpdesk.core.model.ILlmConfig;

/**
 * Thin delegate that calls AWS Bedrock Nova model using the InvokeModel API.
 * - No retries, no timing, no logging: these are handled by the adapter/abstract client.
 * - Returns the raw JSON response for the R client to parse.
 */
public class NovaClientBedrock extends AbstractBedrockDriver {

    private static final Object CLIENT_LOCK = new Object();
    private static BedrockRuntimeClient staticClient;
    private double temperature;
 //   private String region;
    private String modelId;
    private int maxTokens;
    private double topP = 0.9; // Default value for Nova
    private String systemPrompt = "You are an AI help desk assistant for the agricultural management software Conto Terzi. Answer user questions clearly and only about the functionalities and operations of this software. If a question is unrelated or out of scope, politely say you cannot answer."; // Default fallback
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor required for SPI loading.
     * Configuration will be provided via init() method.
     */
    public NovaClientBedrock() {
        // Will be initialized in init()
    }


    /**
     * Performs a single "invokeModel" call to Nova via Bedrock and returns the raw JSON response.
     * This method DOES NOT implement retries and DOES NOT measure time.
     * Exceptions are propagated to be handled by upper layers.
     */
    @Override
    protected LlmResponse callTheLLM(LlmRequest request) throws LlmException {
        if (staticClient == null) {
            throw new IllegalStateException("NovaClientBedrock not initialized. Call init() first.");
        }
        
        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("Request and its prompt cannot be null or empty");
        }



        String prompt = request.getPrompt();

        // Use ObjectMapper for proper JSON construction (Nova format)
        Map<String, Object> payload = Map.of(
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "text", prompt.trim()
                                        )
                                )
                        )
                ),
                "system", List.of(
                        Map.of(
                                "text", systemPrompt != null ? systemPrompt : ""
                        )
                ),
                "inferenceConfig", Map.of(
                        "maxTokens", maxTokens,
                        "temperature", temperature,
                        "topP", topP
                )
        );

        try {
            String jsonPayload = mapper.writeValueAsString(payload);

            InvokeModelRequest req = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

/*            System.out.println("DEBUG: Client region = " + staticClient.serviceClientConfiguration().region());
            System.out.println("DEBUG: About to call Bedrock with modelId = " + modelId);
*/
            // HERE IT CALLS NOVA !
            InvokeModelResponse resp = staticClient.invokeModel(req);
            String body = resp.body().asUtf8String();
            
            // Validation: ensure we got a non-empty response
            if (body == null || body.trim().isEmpty()) {
                throw new RuntimeException("Empty response from Nova API");
            }
            
            // Return the raw JSON response wrapped in LlmResponse
            return new LlmResponse(body);
        } catch (Exception ex) {
            // Let AbstractBedrockDriver handle the exception conversion
            throw new RuntimeException("Nova API call failed", ex);
        }
    }

    @Override
    public String id() {
        return "nova";
    }

    @Override
    public void init(AppConfig config) {
        ILlmConfig llmConfig = config.getLlm();
        this.modelId = llmConfig.getModelId();
        this.maxTokens = llmConfig.getMaxTokens();
        this.temperature = llmConfig.getTemperature();
//        this.region = llmConfig.getRegion();
        // topP keeps default value (0.9)

        // Initialize prompts configuration with null-safety
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
                        .build();
/*
                System.out.println("DEBUG: BedrockRuntimeClient created with region = " + llmConfig.getRegion());
                System.out.println("DEBUG: Client region = " + staticClient.serviceClientConfiguration().region());
*/
            }
        }
    }
}