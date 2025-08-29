package cloud.contoterzi.helpdesk.core.model;

/**
 * Represents the application configuration required for initializing
 * various components of the system, including the large language model,
 * storage, and similarity service.
 */
public interface IAppConfig {
    ILlmConfig getLlm();

    IStorageConfig getStorage();

    ISimilarityConfig getSimilarity();
}
