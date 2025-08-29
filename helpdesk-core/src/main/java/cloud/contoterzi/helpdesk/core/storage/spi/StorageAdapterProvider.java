package cloud.contoterzi.helpdesk.core.storage.spi;

import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * SPI for pluggable StorageAdapter implementations.
 * Implementations are discovered via ServiceLoader.
 */
public interface StorageAdapterProvider {
    /**
     * @return a short, unique id (e.g. "s3", "fs").
     */
    String id();

    /**
     * @return true if this provider can handle the given URI (typically by scheme).
     */
    boolean canHandle(URI uri);

    /**
     * Build a StorageAdapter for the given URI.
     * @param uri like s3://bucket/key or file:///path/to/file.json
     * @param options optional key/values (region, charset, etc.)
     */
    StorageAdapter create(URI uri, Map<String, String> options) throws IOException;
}
