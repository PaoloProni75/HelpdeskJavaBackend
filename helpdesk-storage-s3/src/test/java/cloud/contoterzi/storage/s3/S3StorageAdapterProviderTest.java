package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3StorageAdapterProviderTest {

    private S3StorageAdapterProvider provider;

    @BeforeEach
    void setUp() {
        provider = new S3StorageAdapterProvider();
    }

    @Test
    void testGetId() {
        assertEquals("s3", provider.id());
    }

    @Test
    void testCanHandleValidS3Uri() {
        assertTrue(provider.canHandle(URI.create("s3://my-bucket/path/to/file.json")));
        assertTrue(provider.canHandle(URI.create("S3://my-bucket/file.json")));
        assertTrue(provider.canHandle(URI.create("s3://bucket/key")));
    }

    @Test
    void testCanHandleInvalidUri() {
        assertFalse(provider.canHandle(URI.create("file:///path/to/file.json")));
        assertFalse(provider.canHandle(URI.create("http://example.com/file.json")));
        assertFalse(provider.canHandle(URI.create("ftp://example.com/file.json")));
        assertFalse(provider.canHandle(null));
    }

    @Test
    void testCreateWithValidUri() throws IOException {
        URI uri = URI.create("s3://test-bucket/path/to/knowledge.json");
        Map<String, String> options = Map.of("region", "us-east-1");
        
        StorageAdapter adapter = provider.create(uri, options);
        
        assertNotNull(adapter);
        assertInstanceOf(S3StorageAdapter.class, adapter);
        assertEquals("s3", adapter.type());
    }

    @Test
    void testCreateWithDifferentRegion() throws IOException {
        URI uri = URI.create("s3://test-bucket/kb.json");
        Map<String, String> options = Map.of("region", "eu-central-1");
        
        StorageAdapter adapter = provider.create(uri, options);
        
        assertNotNull(adapter);
        assertEquals("s3", adapter.type());
    }

    @Test
    void testCreateMissingRegion() {
        URI uri = URI.create("s3://test-bucket/file.json");
        Map<String, String> options = Map.of();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Missing 'region' option"));
    }

    @Test
    void testCreateNullOptions() {
        URI uri = URI.create("s3://test-bucket/file.json");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, null));
        
        assertTrue(exception.getMessage().contains("Missing 'region' option"));
    }

    @Test
    void testCreateEmptyRegion() {
        URI uri = URI.create("s3://test-bucket/file.json");
        Map<String, String> options = Map.of("region", "");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Missing 'region' option"));
    }

    @Test
    void testCreateBlankRegion() {
        URI uri = URI.create("s3://test-bucket/file.json");
        Map<String, String> options = Map.of("region", "   ");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Missing 'region' option"));
    }

    @Test
    void testCreateNullUri() {
        Map<String, String> options = Map.of("region", "us-east-1");
        
        NullPointerException exception = assertThrows(NullPointerException.class, () -> 
            provider.create(null, options));
        
        assertEquals("uri", exception.getMessage());
    }

    @Test
    void testCreateUnsupportedScheme() {
        URI uri = URI.create("file:///path/to/file.json");
        Map<String, String> options = Map.of("region", "us-east-1");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Unsupported scheme: file"));
    }

    @Test
    void testCreateMissingBucket() {
        URI uri = URI.create("s3:///path/to/file.json");
        Map<String, String> options = Map.of("region", "us-east-1");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Missing S3 bucket"));
    }

    @Test
    void testCreateMissingKey() {
        URI uri = URI.create("s3://test-bucket");
        Map<String, String> options = Map.of("region", "us-east-1");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            provider.create(uri, options));
        
        assertTrue(exception.getMessage().contains("Missing S3 key"));
    }

    @Test
    void testCreateWithLeadingSlashInKey() throws IOException {
        URI uri = URI.create("s3://test-bucket/path/to/file.json");
        Map<String, String> options = Map.of("region", "us-east-1");
        
        // Should work - leading slash is handled in the provider
        StorageAdapter adapter = provider.create(uri, options);
        assertNotNull(adapter);
    }

    @Test
    void testCreateWithNestedPath() throws IOException {
        URI uri = URI.create("s3://my-bucket/data/v1/knowledge/kb.json");
        Map<String, String> options = Map.of("region", "eu-west-1");
        
        StorageAdapter adapter = provider.create(uri, options);
        assertNotNull(adapter);
        assertEquals("s3", adapter.type());
    }

    @Test
    void testCreateWithAdditionalOptions() throws IOException {
        URI uri = URI.create("s3://test-bucket/file.json");
        Map<String, String> options = Map.of(
            "region", "us-west-2",
            "timeout", "30",
            "retries", "3"
        );
        
        // Should work - additional options are ignored
        StorageAdapter adapter = provider.create(uri, options);
        assertNotNull(adapter);
    }

    @Test
    void testConstants() {
        assertEquals("s3", S3StorageAdapterProvider.ID);
        assertEquals("region", S3StorageAdapterProvider.OPT_REGION);
    }
}