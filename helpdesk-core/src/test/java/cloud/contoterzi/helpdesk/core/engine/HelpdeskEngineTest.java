package cloud.contoterzi.helpdesk.core.engine;

import cloud.contoterzi.helpdesk.core.llm.ProviderException;
import cloud.contoterzi.helpdesk.core.model.*;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelpdeskEngineTest {

    @Test
    void testHelpdeskEngineCreation() {
        HelpdeskEngine engine = new HelpdeskEngine();
        assertNotNull(engine);
    }

    @Test
    void testHelpdeskEngineRequiresInitialization() {
        HelpdeskEngine engine = new HelpdeskEngine();
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("Test question");
        
        // Attempting to process a question without calling init() should fail with IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            engine.processQuestion(request);
        }, "processQuestion() should fail when init() hasn't been called");
        
        // Verify the exception message is helpful
        assertTrue(exception.getMessage().contains("not initialized"), 
                  "Exception message should indicate the engine is not initialized");
        assertTrue(exception.getMessage().contains("init()"), 
                  "Exception message should mention calling init()");
    }

    @Test
    void testKnowledgeBestMatchCreation() {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setQuestion("Test question");
        entry.setAnswer("Test answer");
        entry.setEscalation(false);

        KnowledgeBestMatch match = new KnowledgeBestMatch(false, 0.8, entry);
        
        assertNotNull(match);
        assertEquals(0.8, match.getBestSim());
        assertFalse(match.isShouldInvokeLlm());
        assertEquals(entry, match.getBestKBItem());
    }

    @Test
    void testHelpdeskRequestCreation() {
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("How do I reset my password?");
        
        assertNotNull(request);
        assertEquals("How do I reset my password?", request.getQuestion());
    }

    @Test
    void testKnowledgeEntryCreation() {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setQuestion("What is the system uptime?");
        entry.setAnswer("System has been running for 24 hours.");
        entry.setEscalation(false);
        
        assertNotNull(entry);
        assertEquals("What is the system uptime?", entry.getQuestion());
        assertEquals("System has been running for 24 hours.", entry.getAnswer());
        assertFalse(entry.isEscalation());
    }

    @Test
    void testKnowledgeEntryWithEscalation() {
        KnowledgeEntry entry = new KnowledgeEntry();
        entry.setQuestion("Critical system failure");
        entry.setAnswer("Immediate attention required");
        entry.setEscalation(true);
        
        assertTrue(entry.isEscalation());
        assertEquals("Critical system failure", entry.getQuestion());
        assertEquals("Immediate attention required", entry.getAnswer());
    }

    @Test
    void testLlmExceptionProperties() {
        ProviderException exception = new ProviderException("Service unavailable");
        
        assertEquals("Service unavailable", exception.getMessage());
        assertTrue(exception.isRetryable());
        assertFalse(exception.isNotRetryable());
    }

    @Test
    void testLlmExceptionWithStatusCode() {
        ProviderException exception = new ProviderException("Rate limit exceeded", 429);
        
        assertEquals("Rate limit exceeded", exception.getMessage());
        assertEquals(429, exception.getStatusCode());
        assertTrue(exception.isRetryable());
    }

    @Test
    void testConstantsInHelpdeskEngine() {
        assertEquals("kb", HelpdeskEngine.KB);
        assertEquals("llm", HelpdeskEngine.LLM);
        assertEquals("Sorry, I don't know the answer to that question.", HelpdeskEngine.FALLBACK);
    }

    @Test
    void testHelpdeskResponseBuilderPattern() {
        HelpdeskResponse response = HelpdeskResponse.builder()
                .answer("Test answer")
                .confidence(0.95)
                .escalation(false)
                .action("none")
                .source("kb")
                .responseTimeMs(50L)
                .build();
        
        assertNotNull(response);
        assertEquals("Test answer", response.getAnswer());
        assertEquals(0.95, response.getConfidence());
        assertFalse(response.isEscalation());
        assertEquals("none", response.getAction());
        assertEquals("kb", response.getSource());
        assertEquals(50L, response.getResponseTimeMs());
    }

    @Test
    void testLlmRequestCreation() {
        LlmRequest request = new LlmRequest("What is 2+2?");
        
        assertNotNull(request);
        assertEquals("What is 2+2?", request.getPrompt());
    }

    @Test
    void testLlmResponseCreation() {
        LlmResponse response = new LlmResponse();
        response.setAnswer("4");
        response.setTimeMs(100L);
        
        assertNotNull(response);
        assertEquals("4", response.getAnswer());
        assertEquals(100L, response.getTimeMs());
    }
}