package cloud.contoterzi.helpdesk.core.model.impl;


import cloud.contoterzi.helpdesk.core.model.ISimilarityConfig;

/**
 * Represents the configuration for similarity-based operations.
 * This class encapsulates parameters used for configuring similarity thresholds
 * and the number of examples (few-shot learning) to be considered for processing.
 */
public class SimilarityConfig implements ISimilarityConfig {
    private String type;
    private Integer fewShot;
    private Double threshold;

    public SimilarityConfig() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getFewShot() {
        return fewShot;
    }

    public void setFewShot(Integer fewShot) {
        this.fewShot = fewShot;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }
}
