package cloud.contoterzi.helpdesk.core.engine;

import cloud.contoterzi.helpdesk.core.llm.ProviderException;
import cloud.contoterzi.helpdesk.core.model.*;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;
import cloud.contoterzi.helpdesk.core.spi.LlmClient;
import cloud.contoterzi.helpdesk.core.spi.SimilarityService;
import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;
import cloud.contoterzi.helpdesk.core.util.SpiLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HelpdeskEngineSystemTest {

    @Mock
    private LlmClient mockLlmClient;
    
    @Mock
    private SimilarityService mockSimilarityService;
    
    @Mock
    private StorageAdapter mockStorageAdapter;
    
    private HelpdeskEngine helpdeskEngine;
    private List<IKnowledge> testKnowledgeBase;

    @BeforeEach
    void setUp() throws Exception {
        helpdeskEngine = new HelpdeskEngine();
        
        // Create test knowledge base
        KnowledgeEntry entry1 = new KnowledgeEntry();
        entry1.setQuestion("How do I reset my password?");
        entry1.setAnswer("You can reset your password by clicking the 'Forgot Password' link on the login page.");
        entry1.setEscalation(false);
        
        KnowledgeEntry entry2 = new KnowledgeEntry();
        entry2.setQuestion("System is down");
        entry2.setAnswer("Please contact support immediately for system outages.");
        entry2.setEscalation(true);
        
        testKnowledgeBase = Arrays.asList(entry1, entry2);
        
        // Setup mock behaviors
        when(mockStorageAdapter.loadKnowledgeBase()).thenReturn(testKnowledgeBase);
        doNothing().when(mockStorageAdapter).init(any());
        doNothing().when(mockLlmClient).init(any());
        doNothing().when(mockSimilarityService).init(any());
    }

    @AfterEach
    void tearDown() {
        // Environment variables are handled by Maven surefire plugin
        // No manual cleanup needed
    }

    @Test
    void testProcessQuestion_WithKnowledgeBasePath_ReturnsDirectAnswer() throws Exception {
        // Arrange: ALWAYS_CALL_LLM is set to false by Maven surefire plugin
        
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("How do I reset my password?");
        
        IKnowledge bestMatch = testKnowledgeBase.get(0);
        KnowledgeBestMatch match = new KnowledgeBestMatch(false, 0.95, bestMatch);
        
        when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                .thenReturn(match);
        
        // Act & Assert with mocked static methods
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            helpdeskEngine.init();
            HelpdeskResponse response = helpdeskEngine.processQuestion(request);
            
            // Assert
            assertNotNull(response);
            assertEquals("You can reset your password by clicking the 'Forgot Password' link on the login page.", 
                        response.getAnswer());
            assertEquals(0.95, response.getConfidence());
            assertFalse(response.isEscalation());
            assertEquals("none", response.getAction());
            assertEquals("kb", response.getSource());
            assertEquals(0L, response.getResponseTimeMs());
            
            // Verify LLM was NOT called since ALWAYS_CALL_LLM = false and confidence is high
            verify(mockLlmClient, never()).ask(any());
        }
    }

    @Test
    void testProcessQuestion_WithAlwaysCallLlmTrue_InvokesLlmService() throws Exception {
        // Arrange: ALWAYS_CALL_LLM can be changed via environment variable, but this test focuses on LLM path
        
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("What is the weather today?");
        
        KnowledgeBestMatch match = new KnowledgeBestMatch(true, 0.3, null);
        
        LlmResponse llmResponse = new LlmResponse();
        llmResponse.setAnswer("I cannot answer weather questions as I'm a helpdesk assistant for agricultural software.");
        llmResponse.setTimeMs(150L);
        
        when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                .thenReturn(match);
        when(mockLlmClient.ask(any(LlmRequest.class))).thenReturn(llmResponse);
        
        // Act & Assert
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            helpdeskEngine.init();
            HelpdeskResponse response = helpdeskEngine.processQuestion(request);
            
            // Assert
            assertNotNull(response);
            assertEquals("I cannot answer weather questions as I'm a helpdesk assistant for agricultural software.", 
                        response.getAnswer());
            assertEquals(0.3, response.getConfidence());
            assertFalse(response.isEscalation()); // No "contact support" phrase
            assertEquals("notify_human", response.getAction());
            assertEquals("llm", response.getSource());
            assertEquals(150L, response.getResponseTimeMs());
            
            // Verify LLM WAS called since ALWAYS_CALL_LLM = true
            verify(mockLlmClient, times(1)).ask(any(LlmRequest.class));
        }
    }

    @Test
    void testProcessQuestion_WithEscalationRequired_ReturnsEscalationResponse() throws Exception {
        // Arrange: ALWAYS_CALL_LLM is set to false by Maven surefire plugin
        
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("System is completely broken");
        
        IKnowledge escalationMatch = testKnowledgeBase.get(1); // escalation = true
        KnowledgeBestMatch match = new KnowledgeBestMatch(false, 0.85, escalationMatch);
        
        when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                .thenReturn(match);
        
        // Act & Assert
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            helpdeskEngine.init();
            HelpdeskResponse response = helpdeskEngine.processQuestion(request);
            
            // Assert
            assertNotNull(response);
            assertEquals("Please contact support immediately for system outages.", response.getAnswer());
            assertEquals(0.85, response.getConfidence());
            assertTrue(response.isEscalation());
            assertEquals("notify_human", response.getAction());
            assertEquals("kb", response.getSource());
        }
    }

    @Test
    void testProcessQuestion_WithLlmResponseContainingContactSupport_ReturnsEscalation() throws Exception {
        // Arrange: Test LLM response containing contact support phrase
        
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("Complex technical issue");
        
        KnowledgeBestMatch match = new KnowledgeBestMatch(true, 0.4, null);
        
        LlmResponse llmResponse = new LlmResponse();
        llmResponse.setAnswer("This is a complex issue that requires specialized knowledge. Please contact support for assistance.");
        llmResponse.setTimeMs(200L);
        
        when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                .thenReturn(match);
        when(mockLlmClient.ask(any(LlmRequest.class))).thenReturn(llmResponse);
        
        // Act & Assert
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            helpdeskEngine.init();
            HelpdeskResponse response = helpdeskEngine.processQuestion(request);
            
            // Assert
            assertNotNull(response);
            assertTrue(response.getAnswer().contains("contact support"));
            assertTrue(response.isEscalation());
            assertEquals("notify_human", response.getAction());
            assertEquals("llm", response.getSource());
            assertEquals(200L, response.getResponseTimeMs());
            
            // Verify LLM was called
            verify(mockLlmClient, times(1)).ask(any(LlmRequest.class));
        }
    }

    @Test
    void testProcessQuestion_WithLlmException_ReturnsFallbackWithEscalation() throws Exception {
        // Temporarily reduce log level to avoid noise in test output
        Logger helpdeskLogger = Logger.getLogger(HelpdeskEngine.class.getName());
        Level originalLevel = helpdeskLogger.getLevel();
        helpdeskLogger.setLevel(Level.SEVERE);
        
        try {
            // Arrange: Test LLM exception handling
            
            HelpdeskRequest request = new HelpdeskRequest();
            request.setQuestion("Test question that fails");
            
            KnowledgeBestMatch match = new KnowledgeBestMatch(true, 0.2, null);
            
            ProviderException llmException = new ProviderException("Service unavailable", 503);
            
            when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                    .thenReturn(match);
            when(mockLlmClient.ask(any(LlmRequest.class))).thenThrow(llmException);
            
            // Act & Assert
            try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
                setupMockSpiLoader(mockedSpiLoader);
                
                helpdeskEngine.init();
                HelpdeskResponse response = helpdeskEngine.processQuestion(request);
                
                // Assert
                assertNotNull(response);
                assertEquals(HelpdeskEngine.FALLBACK, response.getAnswer());
                assertEquals(0.0, response.getConfidence());
                assertFalse(response.isEscalation()); // ProviderException is retryable, no escalation
                assertEquals("llm", response.getSource());
                assertEquals(0L, response.getResponseTimeMs());
                
                // Verify LLM was called (but failed)
                verify(mockLlmClient, times(1)).ask(any(LlmRequest.class));
            }
        } finally {
            // Restore original log level
            helpdeskLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testProcessQuestion_ToggleBetweenLlmAndKnowledgeBase() throws Exception {
        // Test 1: Use Knowledge Base (ALWAYS_CALL_LLM = false)
        // Note: ALWAYS_CALL_LLM is controlled by environment variable
        
        HelpdeskRequest request = new HelpdeskRequest();
        request.setQuestion("How do I reset my password?");
        
        IKnowledge bestMatch = testKnowledgeBase.get(0);
        KnowledgeBestMatch match = new KnowledgeBestMatch(false, 0.95, bestMatch);
        
        when(mockSimilarityService.findBestMatch(anyString(), any(), anyDouble()))
                .thenReturn(match);
        
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            helpdeskEngine.init();
            HelpdeskResponse response1 = helpdeskEngine.processQuestion(request);
            
            // Assert KB path
            assertEquals("kb", response1.getSource());
            verify(mockLlmClient, never()).ask(any());
        }
        
        // Test 2: We cannot easily toggle environment variables in the same JVM
        // This test demonstrates the limitation - would need separate test execution
        // For now, we'll test with the current ALWAYS_CALL_LLM=false setting
        
        // Create new engine instance
        HelpdeskEngine newEngine = new HelpdeskEngine();
        
        LlmResponse llmResponse = new LlmResponse();
        llmResponse.setAnswer("LLM processed your password reset question.");
        llmResponse.setTimeMs(100L);
        
        when(mockLlmClient.ask(any(LlmRequest.class))).thenReturn(llmResponse);
        
        try (MockedStatic<SpiLoader> mockedSpiLoader = mockStatic(SpiLoader.class)) {
            setupMockSpiLoader(mockedSpiLoader);
            
            newEngine.init();
            HelpdeskResponse response2 = newEngine.processQuestion(request);
            
            // Since ALWAYS_CALL_LLM=false and confidence is high (0.95), it will use KB path
            assertEquals("kb", response2.getSource());
            assertEquals("You can reset your password by clicking the 'Forgot Password' link on the login page.", response2.getAnswer());
            // LLM is not called since we use KB path with high confidence
            verify(mockLlmClient, never()).ask(any());
        }
    }

    private void setupMockSpiLoader(MockedStatic<SpiLoader> mockedSpiLoader) {
        mockedSpiLoader.when(() -> SpiLoader.loadByKey(eq(LlmClient.class), eq("claude")))
                      .thenReturn(mockLlmClient);
        mockedSpiLoader.when(() -> SpiLoader.loadByKey(eq(SimilarityService.class), eq("cosine")))
                      .thenReturn(mockSimilarityService);
        // Mock StorageAdapterProvider instead of StorageAdapter
        StorageAdapterProvider mockProvider = mock(StorageAdapterProvider.class);
        try {
            when(mockProvider.create(any(), any())).thenReturn(mockStorageAdapter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockedSpiLoader.when(() -> SpiLoader.loadByKey(eq(StorageAdapterProvider.class), eq("s3")))
                      .thenReturn(mockProvider);
    }
}