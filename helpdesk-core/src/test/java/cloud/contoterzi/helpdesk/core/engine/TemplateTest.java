package cloud.contoterzi.helpdesk.core.engine;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per verificare che la logica del template funzioni correttamente.
 */
class TemplateTest {

    @Test
    void testBuildPromptWithTemplate() throws Exception {
        // Arrange
        HelpdeskEngine engine = new HelpdeskEngine();

        // Create test configuration using YAML format
        String yamlContent = """
            llm:
              prompts:
                preamble: "You are a helpful assistant for agricultural software."
                template: "%s%n%nExamples:%n%s%n%nUser question: %s%nAnswer:"
                contactSupportPhrase: "contact support"
            """;

        YamlConfig config = new YamlConfig(new java.io.ByteArrayInputStream(yamlContent.getBytes()));
        
        // Create test knowledge base
        KnowledgeEntry entry1 = new KnowledgeEntry();
        entry1.setQuestion("How do I create a new farm?");
        entry1.setAnswer("Go to Farm Management -> Add New Farm");
        entry1.setEscalation(false);
        
        KnowledgeEntry entry2 = new KnowledgeEntry();
        entry2.setQuestion("How do I add workers?");
        entry2.setAnswer("Navigate to Workers -> Add Worker");
        entry2.setEscalation(false);
        
        List<IKnowledge> testKb = List.of(entry1, entry2);
        
        // Use reflection to access private methods and fields
        Method buildPromptMethod = HelpdeskEngine.class.getDeclaredMethod("buildPromptWithTemplate", String.class);
        buildPromptMethod.setAccessible(true);
        
        // Set private fields
        setPrivateField(engine, "config", config);
        setPrivateField(engine, "kb", testKb);
        
        // Act
        String result = (String) buildPromptMethod.invoke(engine, "How do I delete a farm?");
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("You are a helpful assistant for agricultural software."));
        assertTrue(result.contains("Examples:"));
        assertTrue(result.contains("How do I create a new farm? -> Go to Farm Management -> Add New Farm"));
        assertTrue(result.contains("How do I add workers? -> Navigate to Workers -> Add Worker"));
        assertTrue(result.contains("User question: How do I delete a farm?"));
        assertTrue(result.contains("Answer:"));
        
        // Verify the structure follows the template format
        String[] lines = result.split("\\n");
        assertTrue(lines.length >= 6); // At least preamble + blank + Examples + examples + blank + question + Answer
    }
    
    @Test
    void testBuildPromptWithoutTemplate() throws Exception {
        // Arrange
        HelpdeskEngine engine = new HelpdeskEngine();

        // Create test configuration without template
        String yamlContent = """
            llm:
              prompts:
                preamble: "You are a helpful assistant for agricultural software."
                contactSupportPhrase: "contact support"
            """;

        YamlConfig config = new YamlConfig(new java.io.ByteArrayInputStream(yamlContent.getBytes()));
        
        // Use reflection to access private methods and fields
        Method buildPromptMethod = HelpdeskEngine.class.getDeclaredMethod("buildPromptWithTemplate", String.class);
        buildPromptMethod.setAccessible(true);
        
        // Set private fields
        setPrivateField(engine, "config", config);
        
        // Act
        String result = (String) buildPromptMethod.invoke(engine, "How do I delete a farm?");
        
        // Assert
        assertEquals("How do I delete a farm?", result); // Should return original question
    }
    
    @Test
    void testContactSupportPhraseDetection() throws Exception {
        // Arrange
        HelpdeskEngine engine = new HelpdeskEngine();
        
        // Use reflection to access private method
        Method containsContactSupportMethod = HelpdeskEngine.class.getDeclaredMethod("containsContactSupport", String.class);
        containsContactSupportMethod.setAccessible(true);
        
        // Set contactSupportPhrase field
        setPrivateField(engine, "contactSupportPhrase", "contact support");
        
        // Act & Assert
        assertTrue((Boolean) containsContactSupportMethod.invoke(engine, "Please contact support for this issue."));
        assertTrue((Boolean) containsContactSupportMethod.invoke(engine, "You need to CONTACT SUPPORT immediately."));
        assertFalse((Boolean) containsContactSupportMethod.invoke(engine, "This is a regular answer."));
        assertFalse((Boolean) containsContactSupportMethod.invoke(engine, "Please contact the administrator."));
        assertFalse((Boolean) containsContactSupportMethod.invoke(engine, (String) null));
    }
    
    @Test
    void testCustomContactSupportPhrase() throws Exception {
        // Arrange
        HelpdeskEngine engine = new HelpdeskEngine();
        
        // Use reflection to access private method
        Method containsContactSupportMethod = HelpdeskEngine.class.getDeclaredMethod("containsContactSupport", String.class);
        containsContactSupportMethod.setAccessible(true);
        
        // Set custom contactSupportPhrase
        setPrivateField(engine, "contactSupportPhrase", "escalate to human");
        
        // Act & Assert
        assertTrue((Boolean) containsContactSupportMethod.invoke(engine, "Please escalate to human for this issue."));
        assertFalse((Boolean) containsContactSupportMethod.invoke(engine, "Please contact support for this issue."));
    }
    
    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}