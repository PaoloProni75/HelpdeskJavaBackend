package cloud.contoterzi.helpdesk.core.storage;

import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class StorageAdapterLoader {
    private StorageAdapterLoader() {}

    public static StorageAdapter create(URI uri, Map<String,String> options) throws Exception {
        Objects.requireNonNull(uri, "uri");
        var providers = ServiceLoader.load(StorageAdapterProvider.class);
        List<StorageAdapterProvider> matches = new ArrayList<>();
        for (StorageAdapterProvider p : providers) {
            if (p.canHandle(uri)) matches.add(p);
        }
        if (matches.isEmpty()) {
            String known = discoverIds();
            throw new IllegalArgumentException("No StorageAdapterProvider for URI '" + uri + "'. Known providers: " + known);
        }
        // If more than one matches, it chooses the first one
        return matches.get(0).create(uri, options != null ? options : Map.of());
    }

    public static String discoverIds() {
        var providers = ServiceLoader.load(StorageAdapterProvider.class);
        return StreamSupport.stream(providers.spliterator(), false)
                .map(StorageAdapterProvider::id)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
