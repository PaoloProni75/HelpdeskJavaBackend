package cloud.contoterzi.ollama;

import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaClientTest {

    private OllamaClient client;
    private AppConfig config;

    @BeforeEach
    void setUp() {
        client = new OllamaClient();
        config = new AppConfig();
        
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setType("ollama");
        llmConfig.setEndpoint("http://localhost:11434");
        llmConfig.setModelId("gpt4all");
        llmConfig.setTemperature(0.4);
        llmConfig.setMaxTokens(1024);
        
        PromptsConfig prompts = new PromptsConfig();
        prompts.setPreamble("Test system prompt for Ollama");
        llmConfig.setPrompts(prompts);
        
        config.setLlm(llmConfig);
    }

    @Test
    void testId() {
        assertEquals("ollama", client.id());
    }

    @Test
    void testInit() {
        assertDoesNotThrow(() -> client.init(config));
    }

    @Test
    void testInitWithNullEndpoint() {
        config.getLlm().setEndpoint(null);
        assertThrows(IllegalArgumentException.class, () -> client.init(config));
    }

    @Test
    void testInitWithEmptyEndpoint() {
        config.getLlm().setEndpoint("");
        assertThrows(IllegalArgumentException.class, () -> client.init(config));
    }

    @Test
    void testInitWithDifferentModels() {
        // Test with Mistral
        config.getLlm().setModelId("mistral");
        assertDoesNotThrow(() -> client.init(config));
        
        // Test with Nemotron
        config.getLlm().setModelId("nemotron-mini");
        assertDoesNotThrow(() -> client.init(config));
        
        // Test with Llama2
        config.getLlm().setModelId("llama2");
        assertDoesNotThrow(() -> client.init(config));
    }
}