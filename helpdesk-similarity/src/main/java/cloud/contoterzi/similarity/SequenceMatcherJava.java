package cloud.contoterzi.similarity;

import cloud.contoterzi.helpdesk.core.similarity.SimilarityAlgorithm;

import java.util.*;
import java.util.regex.Pattern;

public class SequenceMatcherJava implements SimilarityAlgorithm {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");

    @Override
    public double compute(String a, String b) {
        if (a == null || b == null || a.trim().isEmpty() || b.trim().isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> vectorA = createWordVector(a.toLowerCase());
        Map<String, Integer> vectorB = createWordVector(b.toLowerCase());

        return cosineSimilarity(vectorA, vectorB);
    }

    private Map<String, Integer> createWordVector(String text) {
        Map<String, Integer> wordCount = new HashMap<>();
        var matcher = WORD_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String word = matcher.group();
            wordCount.merge(word, 1, Integer::sum);
        }
        
        return wordCount;
    }

    private double cosineSimilarity(Map<String, Integer> vectorA, Map<String, Integer> vectorB) {
        if (vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        Set<String> commonWords = new HashSet<>(vectorA.keySet());
        commonWords.retainAll(vectorB.keySet());

        if (commonWords.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        Set<String> allWords = new HashSet<>(vectorA.keySet());
        allWords.addAll(vectorB.keySet());

        for (String word : allWords) {
            int countA = vectorA.getOrDefault(word, 0);
            int countB = vectorB.getOrDefault(word, 0);

            dotProduct += countA * countB;
            magnitudeA += countA * countA;
            magnitudeB += countB * countB;
        }

        if (magnitudeA == 0.0 || magnitudeB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
    }
}