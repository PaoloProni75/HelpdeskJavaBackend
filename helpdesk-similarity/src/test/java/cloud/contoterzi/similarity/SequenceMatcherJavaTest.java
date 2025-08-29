package cloud.contoterzi.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceMatcherJavaTest {

    private SequenceMatcherJava matcher;

    @BeforeEach
    void setUp() {
        matcher = new SequenceMatcherJava();
    }

    @Test
    void testIdenticalStrings() {
        double similarity = matcher.compute("How do I register a new job?", "How do I register a new job?");
        assertEquals(1.0, similarity, 0.0001, "Identical strings should have similarity = 1.0");
    }

    @Test
    void testCompletelyDifferentStrings() {
        double similarity = matcher.compute("How do I register a new job?", "What's the weather like?");
        assertTrue(similarity < 0.5, "Completely different strings should have low similarity");
    }

    @Test
    void testSimilarStrings() {
        double similarity1 = matcher.compute("How do I register a new job?", "How do I create a new job?");
        double similarity2 = matcher.compute("How do I register a new job?", "How can I register a job?");
        
        assertTrue(similarity1 > 0.5, "Similar strings should have high similarity (register vs create)");
        assertTrue(similarity2 > 0.7, "Very similar strings should have very high similarity");
    }

    @Test
    void testCaseInsensitive() {
        double similarity = matcher.compute("HOW DO I REGISTER A NEW JOB?", "how do i register a new job?");
        assertEquals(1.0, similarity, 0.0001, "Case should not affect similarity");
    }

    @Test
    void testNullAndEmpty() {
        assertEquals(0.0, matcher.compute(null, "test"), "Null input should return 0.0");
        assertEquals(0.0, matcher.compute("test", null), "Null input should return 0.0");
        assertEquals(0.0, matcher.compute("", "test"), "Empty string should return 0.0");
        assertEquals(0.0, matcher.compute("test", ""), "Empty string should return 0.0");
        assertEquals(0.0, matcher.compute("   ", "test"), "Whitespace-only string should return 0.0");
    }

    @Test
    void testWordOrderMatters() {
        double similarity = matcher.compute("register new job", "job new register");
        assertTrue(similarity > 0.0, "Same words in different order should still have some similarity");
        assertEquals(1.0, similarity, 0.0001, "Same words should have similarity = 1.0 regardless of order");
    }

    @Test
    void testPartialOverlap() {
        double similarity = matcher.compute("How do I register a job?", "How do I delete a customer?");
        assertTrue(similarity > 0.0 && similarity < 1.0, "Partially overlapping strings should have moderate similarity");
    }

    @Test
    void testKnowledgeBaseExamples() {
        // Test cases based on actual knowledge base entries
        double sim1 = matcher.compute("How do I register a new job?", "How do I create a new job?");
        double sim2 = matcher.compute("How do I register a new job?", "How do I assign a worker to a job?");
        double sim3 = matcher.compute("How do I register a new job?", "What's the best recipe for chocolate cake?");
        
        assertTrue(sim1 > 0.7, "register vs create should be highly similar");
        assertTrue(sim2 > 0.3 && sim2 < 0.7, "job-related questions should be moderately similar");
        assertTrue(sim3 < 0.2, "Job question vs recipe should be very different");
    }

    @Test
    void testAgricultureDomainExamples() {
        double sim1 = matcher.compute("How can I set the work date?", "How do I set the work date, registration date, and payment date?");
        double sim2 = matcher.compute("How do I assign a machine to a job?", "How do I assign a worker to a job?");
        double sim3 = matcher.compute("How do I select the customer for a job?", "How do I select the crop for a job?");
        
        assertTrue(sim1 > 0.6, "Similar work date questions should be similar");
        assertTrue(sim2 > 0.7, "Assignment questions should be similar");
        assertTrue(sim3 > 0.7, "Selection questions should be similar");
    }

    @Test
    void testPunctuationHandling() {
        double similarity = matcher.compute("How do I register a new job?", "How do I register a new job");
        assertTrue(similarity > 0.95, "Punctuation should not significantly affect similarity");
    }

    @Test
    void testRepeatedWords() {
        double similarity = matcher.compute("job job job register", "job register");
        assertTrue(similarity > 0.0, "Repeated words should still contribute to similarity");
    }
}