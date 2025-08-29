package cloud.contoterzi.helpdesk.core.spi;

import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;

import java.util.List;

/**
 * Interface defining a contract for storage adapters that allow loading of knowledge base entries from a specific
 * storage mechanism.
 */
public interface StorageAdapter {
    /**
     * Returns the type of storage system that this adapter is configured for.
     * @return the type of storage system, e.g., "s3", "fs", etc.
     */
    String getType();

    /**
     * Initializes the adapter with the given application configuration.
     * @param appConfig the application configuration to use for initialization
     */
    void init(AppConfig appConfig);

    /**
     * Loads the knowledge base from the storage system.
     * @return a list of knowledge base entries
     */
    List<KnowledgeEntry> loadKnowledgeBase();
}
