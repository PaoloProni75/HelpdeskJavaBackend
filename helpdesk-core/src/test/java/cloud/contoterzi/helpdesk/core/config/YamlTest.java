package cloud.contoterzi.helpdesk.core.config;

import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class YamlTest {

    @Test
    void testPromptsConfigYamlDeserialization() {
        String yamlContent = """
            preamble: Test preamble
            template: Test template
            contactSupportPhrase: test phrase
            """;
        
        Yaml yaml = new Yaml();
        PromptsConfig config = yaml.loadAs(new StringReader(yamlContent), PromptsConfig.class);
        
        assertNotNull(config);
        assertEquals("Test preamble", config.getPreamble());
        assertEquals("Test template", config.getTemplate());
        assertEquals("test phrase", config.getContactSupportPhrase());
    }

    @Test
    void testLlmConfigWithPromptsYamlDeserialization() {
        String yamlContent = """
            type: claude
            region: us-east-1
            modelId: anthropic.claude-3-sonnet-20240229-v1:0
            llmVersion: bedrock-2023-05-31
            temperature: 0.4
            maxTokens: 1024
            prompts:
              preamble: Test help desk assistant
              template: "%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:"
              contactSupportPhrase: contact support
            """;
        
        Yaml yaml = new Yaml();
        LlmConfig config = yaml.loadAs(new StringReader(yamlContent), LlmConfig.class);
        
        assertNotNull(config);
        assertEquals("claude", config.getType());
        assertEquals("us-east-1", config.getRegion());
        assertEquals("anthropic.claude-3-sonnet-20240229-v1:0", config.getModelId());
        assertEquals("bedrock-2023-05-31", config.getLlmVersion());
        assertEquals(0.4, config.getTemperature());
        assertEquals(1024, config.getMaxTokens());
        
        assertNotNull(config.getPrompts());
        assertEquals("Test help desk assistant", config.getPrompts().getPreamble());
        assertEquals("%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:", config.getPrompts().getTemplate());
        assertEquals("contact support", config.getPrompts().getContactSupportPhrase());
    }

    @Test
    void testAppConfigYamlDeserialization() {
        String yamlContent = """
            llm:
              type: claude
              region: us-east-1
              modelId: anthropic.claude-3-sonnet-20240229-v1:0
              llmVersion: bedrock-2023-05-31
              temperature: 0.4
              maxTokens: 1024
              prompts:
                preamble: App config test preamble
                template: App config test template
                contactSupportPhrase: contact support
            storage:
              type: s3
              region: eu-north-1
              bucket: test-bucket
              filename: test-knowledge.json
            similarity:
              type: cosine
              fewShot: 10000
              threshold: 0.8
            """;
        
        Yaml yaml = new Yaml();
        AppConfig config = yaml.loadAs(new StringReader(yamlContent), AppConfig.class);
        
        assertNotNull(config);
        assertNotNull(config.getLlm());
        assertNotNull(config.getStorage());
        assertNotNull(config.getSimilarity());
        
        // Test LLM configuration
        assertEquals("claude", config.getLlm().getType());
        assertEquals("us-east-1", config.getLlm().getRegion());
        
        // Test prompts configuration
        assertNotNull(config.getLlm().getPrompts());
        assertEquals("App config test preamble", config.getLlm().getPrompts().getPreamble());
        assertEquals("App config test template", config.getLlm().getPrompts().getTemplate());
        assertEquals("contact support", config.getLlm().getPrompts().getContactSupportPhrase());
        
        // Test storage configuration
        assertEquals("s3", config.getStorage().getType());
        assertEquals("eu-north-1", config.getStorage().getRegion());
        assertEquals("test-bucket", config.getStorage().getBucket());
        
        // Test similarity configuration
        assertEquals("cosine", config.getSimilarity().getType());
        assertEquals(10000, config.getSimilarity().getFewShot());
        assertEquals(0.8, config.getSimilarity().getThreshold());
    }

    @Test
    void testPromptsConfigSerialization() {
        PromptsConfig config = new PromptsConfig();
        config.setPreamble("Serialization test preamble");
        config.setTemplate("Serialization test template");
        config.setContactSupportPhrase("serialization test phrase");
        
        Yaml yaml = new Yaml();
        String yamlString = yaml.dump(config);
        
        assertNotNull(yamlString);
        assertTrue(yamlString.contains("preamble"));
        assertTrue(yamlString.contains("template"));
        assertTrue(yamlString.contains("contactSupportPhrase"));
    }

    @Test
    void testLlmConfigWithPromptsSerialization() {
        PromptsConfig promptsConfig = new PromptsConfig();
        promptsConfig.setPreamble("LLM serialization test");
        promptsConfig.setTemplate("LLM test template");
        promptsConfig.setContactSupportPhrase("llm test phrase");
        
        LlmConfig llmConfig = new LlmConfig();
        llmConfig.setType("claude");
        llmConfig.setRegion("us-east-1");
        llmConfig.setPrompts(promptsConfig);
        
        Yaml yaml = new Yaml();
        String yamlString = yaml.dump(llmConfig);
        
        assertNotNull(yamlString);
        assertTrue(yamlString.contains("type: claude"));
        assertTrue(yamlString.contains("region: us-east-1"));
        assertTrue(yamlString.contains("prompts:"));
        assertTrue(yamlString.contains("preamble: LLM serialization test"));
    }
}