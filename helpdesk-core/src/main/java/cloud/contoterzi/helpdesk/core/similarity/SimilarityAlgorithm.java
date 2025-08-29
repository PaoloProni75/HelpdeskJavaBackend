package cloud.contoterzi.helpdesk.core.similarity;

public interface SimilarityAlgorithm {
    double compute(String a, String b);
}