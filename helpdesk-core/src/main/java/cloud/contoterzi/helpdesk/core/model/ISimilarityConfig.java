package cloud.contoterzi.helpdesk.core.model;

/**
 * Represents the configuration for similarity-based operations.
 * This class encapsulates parameters used for configuring similarity thresholds
 * and the number of examples (few-shot learning) to be considered for processing.
 */
public interface ISimilarityConfig {
    String getType();
    Integer getFewShot();

    Double getThreshold();
}
