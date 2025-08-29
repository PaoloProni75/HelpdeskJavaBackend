package cloud.contoterzi.aws.nova;

import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NovaClientBedrockTestNew {

    private NovaClientBedrock novaClient;

    @BeforeEach
    void setUp() {
        novaClient = new NovaClientBedrock();
        
        // Create config with inference profile ARN
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("arn:aws:bedrock:eu-south-1:339712862677:inference-profile/eu.amazon.nova-lite-v1:0");
        llmConfig.setMaxTokens(512);
        llmConfig.setTemperature(0.5);
        llmConfig.setRegion("eu-south-1");
        
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("You are an AI help desk assistant for agricultural management software.");
        llmConfig.setPrompts(promptsConfig);
        
        config.setLlm(llmConfig);
        novaClient.init(config);
    }

    @Test
    void testAskWithRealInferenceProfile() {
        // Act & Assert - Test that the method handles AWS calls appropriately
        LlmRequest request = new LlmRequest("How do I create a new job?");
        
        // This would normally make an actual AWS call, so we expect credential/auth errors in CI
        assertThrows(LlmException.class, () -> {
            novaClient.ask(request);
        });
    }

    @Test
    void testInitializationWithInferenceProfile() {
        // Act
        NovaClientBedrock client = new NovaClientBedrock();
        
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("arn:aws:bedrock:eu-south-1:339712862677:inference-profile/eu.amazon.nova-lite-v1:0");
        llmConfig.setMaxTokens(1024);
        llmConfig.setTemperature(0.7);
        llmConfig.setRegion("eu-south-1");
        
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("You are a helpful assistant.");
        llmConfig.setPrompts(promptsConfig);
        
        config.setLlm(llmConfig);
        
        // Assert
        assertDoesNotThrow(() -> client.init(config));
        assertNotNull(client);
    }

    @Test
    void testId() {
        // Act & Assert
        assertEquals("nova", novaClient.id());
    }

    @Test
    void testValidation() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest("")));
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest("   ")));
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest(null)));
    }
}