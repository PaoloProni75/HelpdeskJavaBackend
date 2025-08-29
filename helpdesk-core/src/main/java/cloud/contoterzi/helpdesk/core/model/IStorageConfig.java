package cloud.contoterzi.helpdesk.core.model;

/**
 * Represents the configuration for a storage system.
 * This class contains various parameters to define storage settings
 * such as the type of storage, region, bucket, prefix,
 * path for local file systems, and filename.
 * A subclass could add some additional parameters, such as 'path'
 * if the storage system uses a local file system.
 */
public interface IStorageConfig {
    String getType();

    String getRegion();

    String getBucket();

    String getPrefix();

    String getFilename();
}
