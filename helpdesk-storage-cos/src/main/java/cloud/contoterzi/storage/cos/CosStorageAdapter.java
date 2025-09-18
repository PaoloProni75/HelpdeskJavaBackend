package cloud.contoterzi.storage.cos;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;
import cloud.contoterzi.helpdesk.core.spi.StorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

import java.net.URI;

/**
 * Simplified IBM Cloud Object Storage (COS) Adapter - no URI parsing, direct configuration!
 * This replaces the complex StorageProvider -> URI -> StorageAdapterProvider -> StorageAdapter chain
 * with a simple direct implementation.
 */
public class CosStorageAdapter implements StorageAdapter {
    protected static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private String bucket;
    private String key;
    private S3Client client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "cos";
    }

    @Override
    public boolean supports(String type) {
        return "cos".equals(type);
    }

    @Override
    public void init(YamlConfig config) throws IllegalArgumentException {
        LOGGER.info("Initializing IBM COS Storage Adapter");

        // Direct configuration extraction
        bucket = config.getString("storage.bucket");
        key = config.getString("storage.key");
        String endpoint = config.getString("storage.endpoint");
        String hmacAccessKeyId = config.getString("storage.hmac_access_key_id");
        String hmacSecretAccessKey = config.getString("storage.hmac_secret_access_key");

        // Validate required configuration
        requireNonBlank(bucket, "COS bucket cannot be null or empty");
        requireNonBlank(key,"COS key cannot be null or empty");
        requireNonBlank(endpoint,"COS endpoint cannot be null or empty");
        requireNonBlank(hmacAccessKeyId,"COS HMAC access key ID cannot be null or empty");
        requireNonBlank(hmacSecretAccessKey,"COS HMAC secret access key cannot be null or empty");

        client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(hmacAccessKeyId, hmacSecretAccessKey)))
            .region(Region.EU_CENTRAL_1) // mandatory for the SDK, but th endpoint overrides that
            .endpointOverride(URI.create(endpoint))   // es. https://s3.eu-de.cloud-object-storage.appdomain.cloud
            .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)  // suggested for Cos
                    .build())
            .build();
    }

    @Override
    public List<IKnowledge> loadKnowledgeBase() throws RuntimeException {
        LOGGER.info("Loading knowledge base from COS: cos://{}/{}", bucket, key);

        try {
            // build request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            // download as bytes
            ResponseBytes<GetObjectResponse> objectBytes = client.getObjectAsBytes(getObjectRequest);

            // parse JSON into knowledge list
            byte[] data = objectBytes.asByteArray();
            List<KnowledgeEntry> knowledgeEntries = objectMapper.readValue(
                    data, new TypeReference<List<KnowledgeEntry>>() {}
            );

            // Convert to IKnowledge list
            List<IKnowledge> knowledge = knowledgeEntries.stream()
                    .map(entry -> (IKnowledge) entry)
                    .collect(java.util.stream.Collectors.toList());

            LOGGER.info("Successfully loaded {} knowledge items from COS", knowledge.size());
            return knowledge;

        } catch (Exception e) {
            LOGGER.error("Failed to load knowledge base from COS: {}", e.getMessage());
            throw new RuntimeException("Failed to load knowledge base from COS: cos://%s/%s".formatted(bucket, key), e);
        }
    }

    private static void requireNonBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}