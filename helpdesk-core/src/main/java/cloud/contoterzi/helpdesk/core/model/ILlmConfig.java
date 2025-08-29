package cloud.contoterzi.helpdesk.core.model;

import cloud.contoterzi.helpdesk.core.model.impl.PromptsConfig;

/**
 * Represents the configuration for a large language model (LLM).
 * This class encapsulates various parameters required for interacting
 * with an LLM, such as the model's region, identifier, version,
 * and runtime settings.
 */
public interface ILlmConfig {
    String getType();
    String getRegion();
    String getModelId();
    String getLlmVersion();
    double getTemperature();
    int getMaxTokens();
    IPromptConfig getPrompts();
    String getEndpoint();
}
