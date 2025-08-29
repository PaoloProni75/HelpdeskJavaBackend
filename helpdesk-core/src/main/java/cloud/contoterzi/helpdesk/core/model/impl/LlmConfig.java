package cloud.contoterzi.helpdesk.core.model.impl;


import cloud.contoterzi.helpdesk.core.model.ILlmConfig;
import cloud.contoterzi.helpdesk.core.model.IPromptConfig;

/**
 * Represents the configuration for a large language model (LLM).
 * This class encapsulates various parameters required for interacting
 * with an LLM, such as the model's region, identifier, version,
 * and runtime settings.
 */
public class LlmConfig implements ILlmConfig {
    private String type;
    private String region;
    private String modelId;
    private String llmVersion;
    private double temperature;
    private int maxTokens;
    private String endpoint;

    /**
     * Configuration for prompts.
     */
    private PromptsConfig prompts;

    public LlmConfig() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getLlmVersion() {
        return llmVersion;
    }

    public void setLlmVersion(String llmVersion) {
        this.llmVersion = llmVersion;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public PromptsConfig getPrompts() {
        return prompts;
    }

    public void setPrompts(PromptsConfig prompts) {
        this.prompts = prompts;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
