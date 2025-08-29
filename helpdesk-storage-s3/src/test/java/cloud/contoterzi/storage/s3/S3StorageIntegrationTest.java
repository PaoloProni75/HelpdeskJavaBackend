package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import cloud.contoterzi.helpdesk.core.storage.StorageAdapterFactory;
import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class S3StorageIntegrationTest {

    @Test
    void testServiceLoaderDiscovery() {
        ServiceLoader<StorageAdapterProvider> loader = ServiceLoader.load(StorageAdapterProvider.class);
        
        boolean s3ProviderFound = false;
        for (StorageAdapterProvider provider : loader) {
            if ("s3".equals(provider.id())) {
                s3ProviderFound = true;
                assertInstanceOf(S3StorageAdapterProvider.class, provider);
                break;
            }
        }
        
        assertTrue(s3ProviderFound, "S3StorageAdapterProvider should be discoverable via ServiceLoader");
    }

    @Test
    void testStorageAdapterFactoryDiscovery() {
        List<String> providers = StorageAdapterFactory.discoveredProviders();
        assertTrue(providers.contains("s3"), "S3 provider should be discovered by StorageAdapterFactory");
    }

    @Test
    void testStorageAdapterFactoryCreateS3Adapter() throws IOException {
        String uri = "s3://test-bucket/knowledge.json";
        Map<String, String> options = Map.of("region", "us-east-1");
        
        StorageAdapter adapter = StorageAdapterFactory.fromUri(uri, options);
        
        assertNotNull(adapter);
        assertInstanceOf(S3StorageAdapter.class, adapter);
        assertEquals("s3", adapter.type());
    }

    @Test
    void testStorageAdapterFactoryUnsupportedScheme() {
        String uri = "unknown://test-bucket/knowledge.json";
        Map<String, String> options = Map.of("region", "us-east-1");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            StorageAdapterFactory.fromUri(uri, options));
        
        assertTrue(exception.getMessage().contains("No StorageAdapterProvider found"));
    }

    @Test
    void testS3ProviderCanHandleS3Uris() {
        S3StorageAdapterProvider provider = new S3StorageAdapterProvider();
        
        assertTrue(provider.canHandle(java.net.URI.create("s3://bucket/key")));
        assertFalse(provider.canHandle(java.net.URI.create("file:///path")));
        assertFalse(provider.canHandle(java.net.URI.create("http://example.com")));
    }

    @Test
    void testFactoryWithoutOptions() {
        String uri = "s3://test-bucket/knowledge.json";
        
        // Should fail because region is required
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            StorageAdapterFactory.fromUri(uri));
        
        assertTrue(exception.getMessage().contains("Missing 'region' option"));
    }

    @Test
    void testMultipleS3Adapters() throws IOException {
        // Test that we can create multiple S3 adapters with different configurations
        
        StorageAdapter adapter1 = StorageAdapterFactory.fromUri(
            "s3://bucket1/kb1.json", 
            Map.of("region", "us-east-1")
        );
        
        StorageAdapter adapter2 = StorageAdapterFactory.fromUri(
            "s3://bucket2/kb2.json", 
            Map.of("region", "eu-west-1")
        );
        
        assertNotNull(adapter1);
        assertNotNull(adapter2);
        assertEquals("s3", adapter1.type());
        assertEquals("s3", adapter2.type());
        assertNotSame(adapter1, adapter2);
    }

    @Test
    void testProviderConstantsMatchFactoryUsage() {
        // Verify that the constants in the provider match what the factory expects
        assertEquals("s3", S3StorageAdapterProvider.ID);
        
        // Test that the factory can use the provider ID
        List<String> providers = StorageAdapterFactory.discoveredProviders();
        assertTrue(providers.contains(S3StorageAdapterProvider.ID));
    }

    // Note: Direct StorageAdapter SPI discovery test removed 
    // because we now use StorageAdapterProvider pattern for better performance

    @Test
    void testStorageAdapterProviderSystemWorks() {
        // Test that the optimized StorageAdapterProvider system is functional
        
        ServiceLoader<StorageAdapterProvider> providerLoader = ServiceLoader.load(StorageAdapterProvider.class);
        boolean providerFound = false;
        for (StorageAdapterProvider provider : providerLoader) {
            if ("s3".equals(provider.id())) {
                providerFound = true;
                assertEquals("s3", provider.id());
                assertTrue(provider.canHandle(java.net.URI.create("s3://bucket/key")));
                break;
            }
        }
        assertTrue(providerFound, "StorageAdapterProvider should be discoverable");
    }
}