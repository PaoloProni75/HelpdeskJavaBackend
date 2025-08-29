package cloud.contoterzi.aws.claude;

import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaudeClientBedrockTest {

    private ClaudeClientBedrock claudeClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        claudeClient = new ClaudeClientBedrock();
        
        // Create mock config
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("anthropic.claude-3-sonnet-20240229-v1:0");
        llmConfig.setMaxTokens(1024);
        llmConfig.setLlmVersion("bedrock-2023-05-31");
        llmConfig.setTemperature(0.7);
        llmConfig.setRegion("us-east-1");
        
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("You are a helpful assistant.");
        promptsConfig.setTemplate("%s\n\nQuestion: %s\nAnswer:");
        promptsConfig.setContactSupportPhrase("contact support");
        llmConfig.setPrompts(promptsConfig);
        
        config.setLlm(llmConfig);
        claudeClient.init(config);
    }

    @Test
    void testAskWithValidRequest() {
        // Act & Assert - Test that the method handles AWS credential errors appropriately
        LlmRequest request = new LlmRequest("What is AI?");
        
        // This would normally make an actual AWS call, so we expect credential/auth errors
        assertThrows(LlmException.class, () -> {
            claudeClient.ask(request);
        });
    }

    @Test
    void testAskWithEmptyPrompt() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> claudeClient.callTheLLM(new LlmRequest("")));
        assertThrows(IllegalArgumentException.class, () -> claudeClient.callTheLLM(new LlmRequest("   ")));
        assertThrows(IllegalArgumentException.class, () -> claudeClient.callTheLLM(new LlmRequest(null)));
    }



    @Test
    void testInitialization() {
        // Act
        ClaudeClientBedrock client = new ClaudeClientBedrock();
        
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("anthropic.claude-3-haiku-20240307-v1:0");
        llmConfig.setMaxTokens(512);
        llmConfig.setLlmVersion("bedrock-2023-05-31");
        llmConfig.setTemperature(0.5);
        llmConfig.setRegion("us-west-2");
        
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("You are a helpful assistant.");
        promptsConfig.setTemplate("%s\n\nQuestion: %s\nAnswer:");
        promptsConfig.setContactSupportPhrase("contact support");
        llmConfig.setPrompts(promptsConfig);
        
        config.setLlm(llmConfig);
        
        // Assert
        assertDoesNotThrow(() -> client.init(config));
        assertNotNull(client);
    }


    @Test
    void testInitValidation() {
        ClaudeClientBedrock client = new ClaudeClientBedrock();
        
        // Test null config
        assertThrows(NullPointerException.class, () -> client.init(null));
        
        // Test invalid config - null llm
        AppConfig invalidConfig = new AppConfig();
        assertThrows(NullPointerException.class, () -> client.init(invalidConfig));
        
        // Test valid config
        AppConfig validConfig = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("test-model");
        llmConfig.setMaxTokens(100);
        llmConfig.setLlmVersion("test-version");
        llmConfig.setTemperature(0.7);
        llmConfig.setRegion("us-east-1");
        validConfig.setLlm(llmConfig);
        
        assertDoesNotThrow(() -> client.init(validConfig));
    }

}