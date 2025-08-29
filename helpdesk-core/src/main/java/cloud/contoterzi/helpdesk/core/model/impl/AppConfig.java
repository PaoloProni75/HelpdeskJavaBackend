package cloud.contoterzi.helpdesk.core.model.impl;

import cloud.contoterzi.helpdesk.core.model.IAppConfig;

/**
 * Represents the application configuration required for initializing
 * various components of the system, including the large language model,
 * storage, and similarity service. It also provides an option to
 * control the behavior of always calling the LLM.
 */
public class AppConfig implements IAppConfig {
    /**
     * Configuration for the Long Language Model (LLM).
     */
    private LlmConfig llm;

    /**
     * Configuration for the storage system (e.g., S3, Azure Blob Storage, etc.).
     */
    private StorageConfig storage;

    /**
     * Configuration for the similarity service.
     */
    private SimilarityConfig similarity;



    public AppConfig() {
    }

    /**
     * Constructs an instance of AppConfig with the specified configuration settings.
     *
     * @param llm the configuration details for the large language model (LLM)
     * @param storage the configuration details for the storage system
     * @param similarity the configuration for similarity-based operations
     */
    public AppConfig(LlmConfig llm, StorageConfig storage, SimilarityConfig similarity) {
        this.llm = llm;
        this.storage = storage;
        this.similarity = similarity;
    }

    /**
     * Gets the configuration for the Large Language Model (LLM).
     *
     * @return the LLM configuration settings
     */
    public LlmConfig getLlm() {
        return llm;
    }

    /**
     * Gets the configuration for the storage system.
     *
     * @return the storage system configuration settings
     */
    public StorageConfig getStorage() {
        return storage;
    }

    /**
     * Gets the configuration for the similarity service.
     *
     * @return the similarity service configuration settings
     */
    public SimilarityConfig getSimilarity() {
        return similarity;
    }

    /**
     * Sets the configuration for the Large Language Model (LLM).
     *
     * @param llm the LLM configuration settings to set
     */
    public void setLlm(LlmConfig llm) {
        this.llm = llm;
    }

    /**
     * Sets the configuration for the storage system.
     *
     * @param storage the storage system configuration settings to set
     */
    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    /**
     * Sets the configuration for the similarity service.
     *
     * @param similarity the similarity service configuration settings to set
     */
    public void setSimilarity(SimilarityConfig similarity) {
        this.similarity = similarity;
    }
}