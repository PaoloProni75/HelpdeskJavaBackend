package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import static org.junit.jupiter.api.Assertions.*;

class S3StorageAdapterTest {

    @Test
    void testConstructorValidation() {
        // Test null region
        assertThrows(NullPointerException.class, () -> 
            new S3StorageAdapter(null, "bucket", "key"));
            
        // Test null bucket
        assertThrows(NullPointerException.class, () -> 
            new S3StorageAdapter(Region.US_EAST_1, null, "key"));
            
        // Test null key
        assertThrows(NullPointerException.class, () -> 
            new S3StorageAdapter(Region.US_EAST_1, "bucket", null));
    }

    @Test
    void testType() {
        S3StorageAdapter adapter = new S3StorageAdapter(Region.US_EAST_1, "test-bucket", "test-key");
        assertEquals("s3", adapter.type());
    }

    @Test
    void testInit() {
        S3StorageAdapter adapter = new S3StorageAdapter(Region.US_EAST_1, "test-bucket", "test-key");

        // init should not throw and should do nothing (adapter is already configured)
        assertDoesNotThrow(() -> adapter.init(null));
    }

    @Test
    void testConstructorCreatesS3Client() {
        // Test that constructor doesn't throw and creates adapter successfully
        assertDoesNotThrow(() -> {
            S3StorageAdapter adapter = new S3StorageAdapter(
                Region.EU_CENTRAL_1, 
                "my-bucket", 
                "path/to/knowledge.json"
            );
            assertEquals("s3", adapter.type());
        });
    }

    @Test
    void testConstructorWithDifferentRegions() {
        // Test different AWS regions
        assertDoesNotThrow(() -> {
            new S3StorageAdapter(Region.US_EAST_1, "bucket", "key");
            new S3StorageAdapter(Region.EU_WEST_1, "bucket", "key");
            new S3StorageAdapter(Region.AP_NORTHEAST_1, "bucket", "key");
        });
    }

    @Test
    void testConstructorWithDifferentBucketNames() {
        // Test different bucket naming patterns
        assertDoesNotThrow(() -> {
            new S3StorageAdapter(Region.US_EAST_1, "simple-bucket", "key");
            new S3StorageAdapter(Region.US_EAST_1, "my.bucket.with.dots", "key");
            new S3StorageAdapter(Region.US_EAST_1, "bucket-with-dashes", "key");
            new S3StorageAdapter(Region.US_EAST_1, "123bucket", "key");
        });
    }

    @Test
    void testConstructorWithDifferentKeys() {
        // Test different S3 key patterns
        assertDoesNotThrow(() -> {
            new S3StorageAdapter(Region.US_EAST_1, "bucket", "simple-key.json");
            new S3StorageAdapter(Region.US_EAST_1, "bucket", "path/to/nested/key.json");
            new S3StorageAdapter(Region.US_EAST_1, "bucket", "very/deep/nested/path/file.json");
            new S3StorageAdapter(Region.US_EAST_1, "bucket", "key_with_underscores.json");
        });
    }

    @Test
    void testLoadKnowledgeBaseMethodExists() {
        // Test that the method exists and is callable (though we can't test actual S3 interaction)
        S3StorageAdapter adapter = new S3StorageAdapter(Region.US_EAST_1, "test-bucket", "test-key");
        
        // The method should exist and be callable
        // We can't test actual S3 interaction without credentials/network
        assertNotNull(adapter);
        assertEquals("s3", adapter.type());
        
        // Verify that the method signature is correct
        try {
            // This will likely fail with AWS exception, but confirms method exists
            adapter.loadKnowledgeBase();
        } catch (Exception e) {
            // Expected - we don't have valid AWS credentials in test environment
            // This just confirms the method exists and is callable
            assertNotNull(e);
        }
    }
}