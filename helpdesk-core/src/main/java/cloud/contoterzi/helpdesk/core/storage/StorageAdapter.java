package cloud.contoterzi.helpdesk.core.storage;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import java.io.IOException;
import java.util.List;

/**
 * Interface defining a contract for storage adapters that allow loading
 * of knowledge base entries from a specific storage mechanism.
 *
 * Implementations of this interface must provide mechanisms to fetch
 * and return a list of {@link IKnowledge} objects. The loading process
 * may vary depending on the underlying storage, such as file systems,
 * cloud-based storage, or database systems.
 *
 * This interface is designed to be implemented by classes that need to
 * interact with different storage systems while maintaining a unified
 * access approach.
 */
public interface StorageAdapter {
    /**
     * Returns the type of storage system that this adapter is configured for.
     * @return the type of storage system, e.g., "s3", "fs", etc.
     */
    String type();

    /**
     * Initializes the adapter with the given application configuration.
     * @param appConfig the application configuration to use for initialization
     */
    void init(YamlConfig appConfig);

    /**
     * Loads the knowledge base from the storage system.
     * @return a list of knowledge base entries
     * @throws IOException if there is an error loading the knowledge base
     */
    List<IKnowledge> loadKnowledgeBase() throws IOException;
}