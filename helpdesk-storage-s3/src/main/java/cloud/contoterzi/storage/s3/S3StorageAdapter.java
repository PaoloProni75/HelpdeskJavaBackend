 package cloud.contoterzi.storage.s3;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.impl.KnowledgeEntry;
import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Loads a JSON knowledge base from S3 (AWS SDK v2).
 * Expects the S3 object to be a JSON array of KnowledgeEntry.
 */
public class S3StorageAdapter implements StorageAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String FAILED_TO_LOAD_KB_FROM_S_3_S_S = "Failed to load KB from s3://%s/%s";
    public static final String S_3 = "s3";
    public static final String REGION_MUST_NOT_BE_NULL = "region must not be null";
    public static final String BUCKET_MUST_NOT_BE_NULL = "bucket must not be null";
    public static final String KEY_MUST_NOT_BE_NULL = "key must not be null";

    private final S3Client s3;
    private final String bucket;
    private final String key;

    public S3StorageAdapter(Region region, String bucket, String key) {
        Objects.requireNonNull(region, REGION_MUST_NOT_BE_NULL);
        Objects.requireNonNull(bucket, BUCKET_MUST_NOT_BE_NULL);
        Objects.requireNonNull(key, KEY_MUST_NOT_BE_NULL);

        this.s3 = S3Client.builder()
                .region(region)
                .overrideConfiguration(b -> b
                    .apiCallTimeout(Duration.ofSeconds(60))
                    .apiCallAttemptTimeout(Duration.ofSeconds(30))
                )
                .build();
        this.bucket = bucket;
        this.key    = key ;
    }

    @Override
    public String type() {
        return S_3;
    }

    @Override
    public void init(YamlConfig appConfig) {
        // The S3StorageAdapter is already initialized with region, bucket, and key in the constructor
        // This method is called by the framework but no additional initialization is needed
        // since the S3Client is already configured
    }

    @Override
    public List<IKnowledge> loadKnowledgeBase() throws IOException {
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> resp = s3.getObjectAsBytes(req);

            // JSON array -> List<KnowledgeEntry>
            byte[] bytes = resp.asByteArray();

            return MAPPER.readValue(bytes, new TypeReference<List<KnowledgeEntry>>() {})
                    .stream()
                    .map(IKnowledge.class::cast)
                    .toList();
        } catch (Exception e) {
            throw new IOException(FAILED_TO_LOAD_KB_FROM_S_3_S_S.formatted(bucket, key), e);
        }
    }
}