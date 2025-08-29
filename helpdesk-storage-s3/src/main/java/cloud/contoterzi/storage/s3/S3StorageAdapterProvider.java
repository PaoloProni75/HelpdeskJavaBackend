package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * SPI provider for S3 storage adapters.
 * getId() -> "s3"
 * canHandle() -> true for URIs with scheme "s3"
 */
public final class S3StorageAdapterProvider implements StorageAdapterProvider {

    public static final String ID = "s3";
    public static final String OPT_REGION = "region"; // optional

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean canHandle(URI uri) {
        return uri != null && ID.equalsIgnoreCase(uri.getScheme());
    }

    @Override
    public StorageAdapter create(URI uri, Map<String, String> options) throws IOException {
        Objects.requireNonNull(uri, "uri");
        final String scheme = String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT);
        if (!"s3".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        }
        String bucket = uri.getHost();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Missing S3 bucket in URI host: " + uri);
        }
        String key = uri.getPath();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Missing S3 key in URI path: " + uri);
        }
        if (key.startsWith("/")) key = key.substring(1);

        String regionStr = options != null ? options.get("region") : null;
        if (regionStr == null || regionStr.isBlank()) {
            throw new IllegalArgumentException("Missing 'region' option for S3 adapter");
        }
        Region region = Region.of(regionStr);
        return new S3StorageAdapter(region, bucket, key);
    }
}