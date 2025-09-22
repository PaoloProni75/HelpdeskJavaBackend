package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.spi.StorageAdapter;
import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class S3StorageAdapterTest {

    private S3StorageAdapter adapter;

    @Mock
    private YamlConfig mockConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new S3StorageAdapter();
    }

    @Test
    void testGetType() {
        assertEquals("s3", adapter.getType());
    }

    @Test
    void testSupportsS3() {
        assertTrue(adapter.supports("s3"));
    }

    @Test
    void testDoesNotSupportOtherTypes() {
        assertFalse(adapter.supports("blob"));
        assertFalse(adapter.supports("cos"));
        assertFalse(adapter.supports("file"));
        assertFalse(adapter.supports(null));
        assertFalse(adapter.supports(""));
    }

    @Test
    void testInitMissingBucket() {
        when(mockConfig.getString("storage.bucket")).thenReturn(null);
        when(mockConfig.getString("storage.filename")).thenReturn("test.json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            adapter.init(mockConfig));

        assertTrue(exception.getMessage().contains("S3 bucket cannot be null or empty"));
    }

    @Test
    void testInitEmptyBucket() {
        when(mockConfig.getString("storage.bucket")).thenReturn("");
        when(mockConfig.getString("storage.filename")).thenReturn("test.json");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            adapter.init(mockConfig));

        assertTrue(exception.getMessage().contains("S3 bucket cannot be null or empty"));
    }

    @Test
    void testInitMissingFilename() {
        when(mockConfig.getString("storage.bucket")).thenReturn("test-bucket");
        when(mockConfig.getString("storage.filename")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            adapter.init(mockConfig));

        assertTrue(exception.getMessage().contains("S3 filename cannot be null or empty"));
    }

    @Test
    void testInitEmptyFilename() {
        when(mockConfig.getString("storage.bucket")).thenReturn("test-bucket");
        when(mockConfig.getString("storage.filename")).thenReturn("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            adapter.init(mockConfig));

        assertTrue(exception.getMessage().contains("S3 filename cannot be null or empty"));
    }

    @Test
    void testInitValidConfigWithoutPrefix() {
        when(mockConfig.getString("storage.bucket")).thenReturn("test-bucket");
        when(mockConfig.getString("storage.filename")).thenReturn("knowledge.json");
        when(mockConfig.getString("storage.prefix", "")).thenReturn("");
        when(mockConfig.getString("storage.region", "us-east-1")).thenReturn("us-east-1");

        // With valid config, initialization should succeed (S3 client gets created)
        assertDoesNotThrow(() -> adapter.init(mockConfig));

        // Verify adapter is configured correctly
        assertEquals("s3", adapter.getType());
    }

    @Test
    void testInitValidConfigWithPrefix() {
        when(mockConfig.getString("storage.bucket")).thenReturn("test-bucket");
        when(mockConfig.getString("storage.filename")).thenReturn("knowledge.json");
        when(mockConfig.getString("storage.prefix", "")).thenReturn("data/v1");
        when(mockConfig.getString("storage.region", "us-east-1")).thenReturn("eu-west-1");

        // With valid config, initialization should succeed (S3 client gets created)
        assertDoesNotThrow(() -> adapter.init(mockConfig));

        // Verify adapter is configured correctly
        assertEquals("s3", adapter.getType());
    }

    @Test
    void testLoadKnowledgeBaseWithoutInit() {
        // Should fail because adapter is not initialized
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            adapter.loadKnowledgeBase());

        // Will throw NullPointerException because s3Client is null
        assertInstanceOf(RuntimeException.class, exception);
    }
}