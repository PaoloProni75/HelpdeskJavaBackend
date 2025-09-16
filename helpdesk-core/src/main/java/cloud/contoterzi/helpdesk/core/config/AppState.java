package cloud.contoterzi.helpdesk.core.config;

import cloud.contoterzi.helpdesk.core.model.IKnowledge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.net.URI;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import cloud.contoterzi.helpdesk.core.storage.StorageAdapter;
import cloud.contoterzi.helpdesk.core.storage.spi.StorageAdapterProvider;
import cloud.contoterzi.helpdesk.core.util.SpiLoader;


public enum AppState {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(AppState.class.getName());
    private static final String ALWAYS_CALL_LLM_ENV_VAR = "ALWAYS_CALL_LLM";
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private YamlConfig config;
    private static List<IKnowledge> knowledge;
    private boolean alwaysCallLlm;

    public void init() throws IOException {
        if (initialized.compareAndSet(false, true)) {
            LOGGER.info("Initializing AppState");
            this.config = loadConfigFromEnv();
            String storageType = config.getString("storage.type");
            final StorageAdapterProvider provider = SpiLoader.loadByKey(StorageAdapterProvider.class, storageType);
            final StorageAdapter storage = provider.create(buildStorageUri(config), buildStorageOptions(config));
            storageInit(storage, config);
            knowledge = safeLoadKnownledgeBase(storage);
            final String env = System.getenv(ALWAYS_CALL_LLM_ENV_VAR);
            this.alwaysCallLlm = env != null && env.equalsIgnoreCase("true");
            LOGGER.info("AppState initialized");
        }
    }

    private static YamlConfig loadConfigFromEnv() {
        String path = System.getenv("APP_CONFIG_PATH");
        if (path == null || path.isBlank()) throw new IllegalStateException("APP_CONFIG_PATH not set");

        try (InputStream in = getConfigInputStream(path)) {
            YamlConfig config = new YamlConfig(in);
            LOGGER.info("DEBUG: YAML loaded successfully. Raw data: " + config.getRawData());
            LOGGER.info("DEBUG: Storage section: " + config.get("storage"));
            return config;
        } catch (Exception e) {
            LOGGER.severe("DEBUG: Config loading failed with error: " + e.getMessage());
            throw new IllegalStateException("Cannot read config: %s".formatted(path), e);
        }
    }

    /**
     * Get InputStream for config file, supporting both filesystem paths and classpath resources
     */
    private static InputStream getConfigInputStream(String path) throws IOException {
        LOGGER.info("DEBUG: Attempting to load config from: " + path);

        if (path.startsWith("classpath:")) {
            LOGGER.info("AppState loading the configuration from a classpath resource");
            String resourcePath = path.substring("classpath:".length());
            LOGGER.info("DEBUG: Resource path: " + resourcePath);

            InputStream resourceStream = AppState.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                LOGGER.severe("DEBUG: Resource not found on classpath: " + resourcePath);
                LOGGER.info("DEBUG: Trying to load from current directory as fallback");
                // Fallback: try to load from current directory
                try {
                    return Files.newInputStream(Path.of(resourcePath));
                } catch (IOException fallbackEx) {
                    LOGGER.severe("DEBUG: Fallback also failed: " + fallbackEx.getMessage());
                    throw new IOException("Resource not found on classpath: " + resourcePath + ", fallback also failed: " + fallbackEx.getMessage());
                }
            }
            LOGGER.info("DEBUG: Successfully loaded from classpath");
            return resourceStream;
        } else {
            LOGGER.info("AppState loading the configuration from a file");
            LOGGER.info("DEBUG: File path: " + path);
            return Files.newInputStream(Path.of(path));
        }
    }

    private static void storageInit(StorageAdapter storage, YamlConfig config) {
        try {
            storage.init(config);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<IKnowledge> safeLoadKnownledgeBase(StorageAdapter s) {
        try {
            return s.loadKnowledgeBase();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the application configuration.
     * @return The application configuration.
     */
    public YamlConfig getConfiguration() throws IOException {
        if (!initialized.get())
            init();

        return config;
    }

    /**
     * Returns the list of knowledge entries.
     * @return The list of knowledge entries.
     */
    public List<IKnowledge> getKnowledgeBase()  throws IOException {
        if (!initialized.get())
            init();

        return knowledge;
    }

    public boolean isAlwaysCallLlm() throws IOException {
        if (!initialized.get())
            init();

        return alwaysCallLlm;
    }

    private static URI buildStorageUri(YamlConfig config) {
        String storageType = config.getString("storage.type");
        LOGGER.info("DEBUG: Storage type = " + storageType);

        if ("blob".equals(storageType)) {
            String container = config.getString("storage.container");
            String blob = config.getString("storage.blob");
            LOGGER.info("DEBUG: blob container = " + container + ", blob = " + blob);

            if (container == null || blob == null) {
                LOGGER.severe("DEBUG: Configuration error - container=" + container + ", blob=" + blob);
                LOGGER.info("DEBUG: Raw YAML data = " + config.getRawData());
                throw new IllegalStateException("Storage container and blob must be configured for blob");
            }

            return URI.create("blob://" + container + "/" + blob);
        } else if ("s3".equals(storageType)) {
            String bucket = config.getString("storage.bucket");
            String prefix = config.getString("storage.prefix", "");
            String filename = config.getString("storage.filename");

            if (bucket == null || filename == null) {
                throw new IllegalStateException("Storage bucket and filename must be configured for s3");
            }

            String key = (prefix != null && !prefix.trim().isEmpty())
                    ? prefix.trim() + "/" + filename.trim()
                    : filename.trim();

            if (key.startsWith("/")) key = key.substring(1);

            return URI.create("s3://" + bucket + "/" + key);
        } else {
            throw new IllegalStateException("Unsupported storage type: " + storageType);
        }
    }

    private static Map<String, String> buildStorageOptions(YamlConfig config) {
        String storageType = config.getString("storage.type");

        if ("blob".equals(storageType)) {
            String connectionString = config.getString("storage.connectionString");
            if (connectionString == null) {
                throw new IllegalStateException("Storage connectionString must be configured for azure-blob");
            }
            return Map.of("connectionString", connectionString);
        } else if ("s3".equals(storageType)) {
            String region = config.getString("storage.region");
            if (region == null) {
                throw new IllegalStateException("Storage region must be configured for s3");
            }
            return Map.of("region", region);
        } else {
            throw new IllegalStateException("Unsupported storage type: " + storageType);
        }
    }

}
