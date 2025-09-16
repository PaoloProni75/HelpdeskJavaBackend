package cloud.contoterzi.similarity;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.KnowledgeBestMatch;
import cloud.contoterzi.helpdesk.core.similarity.SimilarityAlgorithm;
import cloud.contoterzi.helpdesk.core.spi.SimilarityService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service provider implementation for cosine similarity using SequenceMatcherJava.
 */
public class CosineSequenceMatcherService implements SimilarityService {

    private SimilarityAlgorithm algorithm;
    
    public CosineSequenceMatcherService() {
        this.algorithm = new SequenceMatcherJava();
    }

    @Override
    public String id() {
        return "cosine";
    }

    @Override
    public void init(YamlConfig appConfig) {
        // No special initialization needed for cosine similarity
    }

    @Override
    public KnowledgeBestMatch findBestMatch(String question, List<IKnowledge> kb, double threshold) {
        if (question == null || question.trim().isEmpty() || kb == null || kb.isEmpty()) {
            return new KnowledgeBestMatch(true, 0.0, null);
        }

        IKnowledge bestMatch = null;
        double bestSimilarity = 0.0;

        for (IKnowledge entry : kb) {
            double similarity = algorithm.compute(question, entry.getQuestion());
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = entry;
            }
        }

        boolean shouldInvokeLlm = bestSimilarity < threshold;
        IKnowledge resultMatch = bestSimilarity >= threshold ? bestMatch : null;

        return new KnowledgeBestMatch(shouldInvokeLlm, bestSimilarity, resultMatch);
    }

    @Override
    public List<IKnowledge> topK(String question, List<IKnowledge> kb, int topK) {
        if (question == null || question.trim().isEmpty() || kb == null || kb.isEmpty() || topK <= 0) {
            return List.of();
        }

        return kb.stream()
                .map(entry -> new ScoredEntry(entry, algorithm.compute(question, entry.getQuestion())))
                .sorted(Comparator.comparingDouble(ScoredEntry::getScore).reversed())
                .limit(topK)
                .map(ScoredEntry::getEntry)
                .collect(Collectors.toList());
    }

    private static class ScoredEntry {
        private final IKnowledge entry;
        private final double score;

        public ScoredEntry(IKnowledge entry, double score) {
            this.entry = entry;
            this.score = score;
        }

        public IKnowledge getEntry() {
            return entry;
        }

        public double getScore() {
            return score;
        }
    }
}