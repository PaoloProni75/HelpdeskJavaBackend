package cloud.contoterzi.aws.claude;

import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.spi.LlmClient;
import cloud.contoterzi.helpdesk.core.util.SpiLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpiLoadingTest {

    @Test
    void testSpiLoading() {
        // Act - Load via SPI
        LlmClient client = SpiLoader.loadByKey(LlmClient.class, "claude");

        // Assert
        assertNotNull(client);
        assertInstanceOf(ClaudeClientBedrock.class, client);
        assertEquals("claude", client.id());
    }

    @Test
    void testInitialization() {
        // Arrange - Create mock config
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setType("claude");
        llmConfig.setRegion("us-east-1");
        llmConfig.setModelId("anthropic.claude-3-sonnet-20240229-v1:0");
        llmConfig.setLlmVersion("bedrock-2023-05-31");
        llmConfig.setTemperature(0.4);
        llmConfig.setMaxTokens(1024);

        AppConfig config = new AppConfig();
        config.setLlm(llmConfig);

        // Act - Load and initialize
        LlmClient client = SpiLoader.loadByKey(LlmClient.class, "claude");
        assertDoesNotThrow(() -> client.init(config));

        // Assert - Should be properly initialized
        assertEquals("claude", client.id());
    }
}