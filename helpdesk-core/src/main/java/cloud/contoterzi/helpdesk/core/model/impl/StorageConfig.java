package cloud.contoterzi.helpdesk.core.model.impl;


import cloud.contoterzi.helpdesk.core.model.IStorageConfig;

/**
 * Represents the configuration for a storage system.
 * This class contains various parameters to define storage settings
 * such as the type of storage, region, bucket, prefix,
 * path for local file systems, and filename.
 * A subclass could add some additional parameters, such as 'path'
 * if the storage system uses a local file system.
 */
public class StorageConfig implements IStorageConfig {
    /**
     * s3, fs etc...
     */
    private String type;
    /**
     * When relevant for a cloud-based storage system, the region.
     */
    private String region;
    /**
     * Name of the bucket, a technology used to organize files in a cloud-based storage system.
      */
    private String bucket;
    /**
     * Name of the directory or file prefix, it can be used to organize files, it can be a path.
     */
    private String prefix;
    /**
     * Name of the resource with the storage configuration.
     */
    private String filename;

    public StorageConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
}
