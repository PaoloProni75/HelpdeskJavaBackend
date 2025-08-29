package cloud.contoterzi.helpdesk.core.config;

import cloud.contoterzi.helpdesk.core.model.IPromptConfig;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;
import cloud.contoterzi.helpdesk.core.model.impl.SimilarityConfig;
import cloud.contoterzi.helpdesk.core.model.impl.StorageConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptsConfigTest {

    @Test
    void testPromptsConfigCreation() {
        PromptsConfig config = new PromptsConfig();
        assertNotNull(config);
        assertNull(config.getPreamble());
        assertNull(config.getTemplate());
        assertNull(config.getContactSupportPhrase());
    }

    @Test
    void testPromptsConfigSettersAndGetters() {
        PromptsConfig config = new PromptsConfig();
        
        String preamble = "You are a helpful assistant.";
        String template = "%s\n\nQuestion: %s\nAnswer:";
        String contactPhrase = "contact support";
        
        config.setPreamble(preamble);
        config.setTemplate(template);
        config.setContactSupportPhrase(contactPhrase);
        
        assertEquals(preamble, config.getPreamble());
        assertEquals(template, config.getTemplate());
        assertEquals(contactPhrase, config.getContactSupportPhrase());
    }

    @Test
    void testPromptsConfigImplementsInterface() {
        PromptsConfig config = new PromptsConfig();
        assertInstanceOf(IPromptConfig.class, config);
        
        config.setPreamble("Test preamble");
        config.setTemplate("Test template");
        config.setContactSupportPhrase("test phrase");
        
        IPromptConfig interfaceRef = config;
        assertEquals("Test preamble", interfaceRef.getPreamble());
        assertEquals("Test template", interfaceRef.getTemplate());
        assertEquals("test phrase", interfaceRef.getContactSupportPhrase());
    }

    @Test
    void testAppConfigWithPromptsConfig() {
        LlmConfig llmConfig = new LlmConfig();
        StorageConfig storageConfig = new StorageConfig();
        SimilarityConfig similarityConfig = new SimilarityConfig();
        PromptsConfig promptsConfig = new PromptsConfig();
        
        promptsConfig.setPreamble("You are a help desk assistant for agricultural software.");
        promptsConfig.setTemplate("%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:");
        promptsConfig.setContactSupportPhrase("contact support");
        
        llmConfig.setPrompts(promptsConfig);
        AppConfig appConfig = new AppConfig(llmConfig, storageConfig, similarityConfig);
        
        assertNotNull(appConfig.getLlm().getPrompts());
        assertEquals(promptsConfig, appConfig.getLlm().getPrompts());
        assertEquals("You are a help desk assistant for agricultural software.", 
                    appConfig.getLlm().getPrompts().getPreamble());
        assertEquals("%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:", 
                    appConfig.getLlm().getPrompts().getTemplate());
        assertEquals("contact support", appConfig.getLlm().getPrompts().getContactSupportPhrase());
    }

    @Test
    void testAppConfigSetPromptsConfig() {
        AppConfig appConfig = new AppConfig();
        LlmConfig llmConfig = new LlmConfig();
        PromptsConfig promptsConfig = new PromptsConfig();
        
        promptsConfig.setPreamble("Test preamble");
        promptsConfig.setTemplate("Test template");
        promptsConfig.setContactSupportPhrase("test contact");
        
        llmConfig.setPrompts(promptsConfig);
        appConfig.setLlm(llmConfig);
        
        assertNotNull(appConfig.getLlm().getPrompts());
        assertEquals("Test preamble", appConfig.getLlm().getPrompts().getPreamble());
        assertEquals("Test template", appConfig.getLlm().getPrompts().getTemplate());
        assertEquals("test contact", appConfig.getLlm().getPrompts().getContactSupportPhrase());
    }

    @Test
    void testPromptsConfigWithMultilineText() {
        PromptsConfig config = new PromptsConfig();
        
        String multilinePreamble = "You are a help desk assistant for an agricultural subcontractor management software.\n" +
                                  "Answer clearly and helpfully only if the question is relevant to the software.\n" +
                                  "If it is not, state that you cannot answer.";
        
        config.setPreamble(multilinePreamble);
        
        assertEquals(multilinePreamble, config.getPreamble());
        assertTrue(config.getPreamble().contains("agricultural subcontractor"));
        assertTrue(config.getPreamble().contains("state that you cannot answer"));
    }

    @Test
    void testPromptsConfigWithFormattingCharacters() {
        PromptsConfig config = new PromptsConfig();
        
        String templateWithFormatting = "%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:";
        
        config.setTemplate(templateWithFormatting);
        
        assertEquals(templateWithFormatting, config.getTemplate());
        assertTrue(config.getTemplate().contains("%s"));
        assertTrue(config.getTemplate().contains("%n"));
    }

    @Test
    void testPromptsConfigNullValues() {
        PromptsConfig config = new PromptsConfig();
        
        config.setPreamble(null);
        config.setTemplate(null);
        config.setContactSupportPhrase(null);
        
        assertNull(config.getPreamble());
        assertNull(config.getTemplate());
        assertNull(config.getContactSupportPhrase());
    }

    @Test
    void testPromptsConfigEmptyValues() {
        PromptsConfig config = new PromptsConfig();
        
        config.setPreamble("");
        config.setTemplate("");
        config.setContactSupportPhrase("");
        
        assertEquals("", config.getPreamble());
        assertEquals("", config.getTemplate());
        assertEquals("", config.getContactSupportPhrase());
    }

    @Test
    void testLlmConfigWithPromptsConfig() {
        LlmConfig llmConfig = new LlmConfig();
        PromptsConfig promptsConfig = new PromptsConfig();
        
        promptsConfig.setPreamble("LLM Test preamble");
        promptsConfig.setTemplate("LLM Test template");
        promptsConfig.setContactSupportPhrase("llm test contact");
        
        llmConfig.setPrompts(promptsConfig);
        
        assertNotNull(llmConfig.getPrompts());
        assertEquals("LLM Test preamble", llmConfig.getPrompts().getPreamble());
        assertEquals("LLM Test template", llmConfig.getPrompts().getTemplate());
        assertEquals("llm test contact", llmConfig.getPrompts().getContactSupportPhrase());
    }

    @Test
    void testLlmConfigPromptsInterface() {
        LlmConfig llmConfig = new LlmConfig();
        PromptsConfig promptsConfig = new PromptsConfig();
        
        promptsConfig.setPreamble("Interface test preamble");
        promptsConfig.setTemplate("Interface test template");
        promptsConfig.setContactSupportPhrase("interface test contact");
        
        llmConfig.setPrompts(promptsConfig);
        
        IPromptConfig promptInterface = llmConfig.getPrompts();
        assertNotNull(promptInterface);
        assertEquals("Interface test preamble", promptInterface.getPreamble());
        assertEquals("Interface test template", promptInterface.getTemplate());
        assertEquals("interface test contact", promptInterface.getContactSupportPhrase());
    }
}