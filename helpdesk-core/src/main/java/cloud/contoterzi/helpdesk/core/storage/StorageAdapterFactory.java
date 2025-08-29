package cloud.contoterzi.helpdesk.core.storage;

import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.ServiceLoader;

public final class StorageAdapterFactory {

    private static final ServiceLoader<StorageAdapterProvider> LOADER =
            ServiceLoader.load(StorageAdapterProvider.class);

    private StorageAdapterFactory() {}

    /**
     * Create a StorageAdapter from a URI using ServiceLoader-discovered providers.
     * Example URIs:
     *  - s3://my-bucket/path/to/kb.json
     *  - file:///var/data/kb.json
     */
    public static StorageAdapter fromUri(String uriString) throws IOException {
        return fromUri(uriString, Map.of());
    }

    public static StorageAdapter fromUri(String uriString, Map<String, String> options) throws IOException {
        Objects.requireNonNull(uriString, "uriString must not be null");
        URI uri = URI.create(uriString);

        for (StorageAdapterProvider p : LOADER) {
            if (p.canHandle(uri)) {
                return p.create(uri, options == null ? Map.of() : options);
            }
        }

        throw new IllegalArgumentException("No StorageAdapterProvider found for URI: " + uriString);
    }

    /**
     * Utility to list discovered providers (useful for debugging).
     */
    public static List<String> discoveredProviders() {
        List<String> out = new ArrayList<>();
        for (StorageAdapterProvider p : LOADER) out.add(p.id());
        return out;
    }
}
