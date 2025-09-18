package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.spi.StorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;
import java.util.logging.Logger;

/**
 * Simplified S3 Storage Adapter - no URI parsing, direct configuration!
 * This replaces the complex StorageProvider -> URI -> StorageAdapterProvider -> StorageAdapter chain
 * with a simple direct implementation.
 */
public class S3StorageAdapter implements StorageAdapter {
    private static final Logger LOGGER = Logger.getLogger(S3StorageAdapter.class.getName());

    private S3Client s3Client;
    private String bucket;
    private String key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "s3";
    }

    @Override
    public boolean supports(String type) {
        return "s3".equals(type);
    }

    @Override
    public void init(YamlConfig config) throws IllegalArgumentException {
        LOGGER.info("Initializing S3 Storage Adapter");

        // Direct configuration extraction - no URI parsing!
        this.bucket = config.getString("storage.bucket");
        String filename = config.getString("storage.filename");
        String prefix = config.getString("storage.prefix", "");
        String region = config.getString("storage.region", "us-east-1");

        // Validate required configuration
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 bucket cannot be null or empty");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 filename cannot be null or empty");
        }

        // Build S3 key directly
        if (prefix != null && !prefix.trim().isEmpty()) {
            this.key = prefix.trim() + "/" + filename.trim();
        } else {
            this.key = filename.trim();
        }

        // Remove leading slash if present
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        // Initialize S3 client directly
        try {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            LOGGER.info("S3 Storage Adapter initialized: bucket=" + bucket + ", key=" + key + ", region=" + region);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to initialize S3 client: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IKnowledge> loadKnowledgeBase() throws RuntimeException {
        LOGGER.info("Loading knowledge base from S3: s3://" + bucket + "/" + key);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getObjectRequest);

            // Parse JSON directly to knowledge objects
            List<IKnowledge> knowledge = objectMapper.readValue(responseStream, new TypeReference<List<IKnowledge>>() {});

            LOGGER.info("Successfully loaded " + knowledge.size() + " knowledge items from S3");
            return knowledge;

        } catch (Exception e) {
            LOGGER.severe("Failed to load knowledge base from S3: " + e.getMessage());
            throw new RuntimeException("Failed to load knowledge base from S3: s3://" + bucket + "/" + key, e);
        }
    }
}