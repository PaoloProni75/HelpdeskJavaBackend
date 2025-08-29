package cloud.contoterzi.aws.nova;

import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NovaClientBedrockTest {

    private NovaClientBedrock novaClient;

    @BeforeEach
    void setUp() {
        novaClient = new NovaClientBedrock();
        
        // Create mock config
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("amazon.nova-lite-v1");
        llmConfig.setMaxTokens(512);
        llmConfig.setTemperature(0.5);
        llmConfig.setRegion("eu-south-1");
        
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("You are a helpful assistant.");
        llmConfig.setPrompts(promptsConfig);
        
        config.setLlm(llmConfig);
        novaClient.init(config);
    }

    @Test
    void testAskWithValidRequest() {
        // Act & Assert - Test that the method handles AWS credential errors appropriately
        LlmRequest request = new LlmRequest("What is AI?");
        
        // This would normally make an actual AWS call, so we expect credential/auth errors
        assertThrows(LlmException.class, () -> {
            novaClient.ask(request);
        });
    }

    @Test
    void testAskWithEmptyPrompt() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest("")));
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest("   ")));
        assertThrows(IllegalArgumentException.class, () -> novaClient.callTheLLM(new LlmRequest(null)));
    }

    @Test
    void testInitialization() {
        // Act
        NovaClientBedrock client = new NovaClientBedrock();
        
        AppConfig config = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setModelId("amazon.nova-lite-v1");
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
    void testInitWithNullConfig() {
        // Act & Assert
        NovaClientBedrock client = new NovaClientBedrock();
        assertThrows(Exception.class, () -> client.init(null));
    }

    // Note: Testing null region is problematic due to static client sharing
    // The validation only occurs on first initialization
}