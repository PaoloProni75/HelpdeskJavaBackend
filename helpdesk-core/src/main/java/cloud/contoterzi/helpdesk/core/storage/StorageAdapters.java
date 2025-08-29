package cloud.contoterzi.helpdesk.core.storage;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;

/**
 * Utility class for managing and creating {@link StorageAdapter} instances.
 * This class is responsible for resolving and delegating the creation of
 * {@link StorageAdapter} instances based on a given URI and scheme.
 *
 * The `StorageAdapters` class uses a {@link ServiceLoader}-based mechanism to
 * dynamically discover and use implementations of the {@link StorageAdapterProvider}
 * interface. Each {@link StorageAdapterProvider} is expected to register its implementation
 * and declare the scheme it supports.
 *
 * This class is not instantiable.
 */
public class StorageAdapters {

    public static final String NULL_URI_ERROR_MESSAGE = "uri must not be null";
    public static final String NO_PROVIDER_FOR_SCHEME_MESSAGE = "No StorageAdapterProvider for scheme: %s";

    private StorageAdapters() {}

    public static StorageAdapter from(URI uri, Map<String, String> props) throws IOException {
        if (uri == null) throw new IllegalArgumentException(NULL_URI_ERROR_MESSAGE);
        String wanted = uri.getScheme().toLowerCase(Locale.ROOT);

        for (StorageAdapterProvider p : ServiceLoader.load(StorageAdapterProvider.class)) {
            if (wanted.equals(p.id().toLowerCase(Locale.ROOT))) {
                return p.create(uri, props == null ? Map.of() : props);
            }
        }
        throw new IllegalArgumentException(NO_PROVIDER_FOR_SCHEME_MESSAGE.formatted(wanted));
    }
}
