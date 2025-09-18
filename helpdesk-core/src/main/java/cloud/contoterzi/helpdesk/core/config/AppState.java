package cloud.contoterzi.helpdesk.core.config;

import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.spi.StorageAdapter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum AppState {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(AppState.class.getName());
    private static final String ALWAYS_CALL_LLM_ENV_VAR = "ALWAYS_CALL_LLM";
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private YamlConfig config;
    private static List<IKnowledge> knowledge;
    private boolean alwaysCallLlm;

    public void init() throws IOException {
        if (initialized.compareAndSet(false, true)) {
            LOGGER.info("Initializing AppState");
            this.config = loadConfigFromEnv();

            // Simplified: Direct storage adapter loading via SPI
            String storageType = config.getString("storage.type");
            LOGGER.info("DEBUG: Storage type = " + storageType);

            ServiceLoader<StorageAdapter> providers = ServiceLoader.load(StorageAdapter.class);

            for (StorageAdapter adapter : providers) {
                if (adapter.supports(storageType)) {
                    LOGGER.info("Found storage adapter: {} for type: {}", adapter.getClass().getSimpleName(), storageType);
                    try {
                        adapter.init(config);
                        knowledge = adapter.loadKnowledgeBase();
                        break;
                    } catch (Exception e) {
                        LOGGER.error("Storage adapter initialization failed for type '{}': {}", storageType, e.getMessage());
                        throw new IllegalStateException("Failed to initialize storage adapter for type '" + storageType + "'", e);
                    }
                }
            }

            if (knowledge == null)
                raiseNoAdapterFound(storageType);

            final String env = System.getenv(ALWAYS_CALL_LLM_ENV_VAR);
            this.alwaysCallLlm = env != null && env.equalsIgnoreCase("true");
            LOGGER.info("AppState initialized");
        }
    }

    /**
     * No adapter found - list available for debugging
     */
    private void raiseNoAdapterFound(String storageType) {
        StringBuilder availableAdapters = new StringBuilder();
        for (StorageAdapter adapter : ServiceLoader.load(StorageAdapter.class)) {
            if (!availableAdapters.isEmpty()) {
                availableAdapters.append(", ");
            }
            availableAdapters.append(adapter.getType());
        }

        StringBuilder message = new StringBuilder("No storage adapter found for type: ");
        message.append(storageType);
        if (!availableAdapters.isEmpty())
            message.append(". Available adapters: ").append(availableAdapters);
        else
            message.append(". No storage adapters are registered via SPI.");

        throw new IllegalStateException(message.toString());
    }

    private static YamlConfig loadConfigFromEnv() {
        String path = System.getenv("APP_CONFIG_PATH");
        if (path == null || path.isBlank()) throw new IllegalStateException("APP_CONFIG_PATH not set");

        try (InputStream in = getConfigInputStream(path)) {
            YamlConfig config = new YamlConfig(in);
            LOGGER.info("DEBUG: YAML loaded successfully. Raw data: {}", config.getRawData());
            LOGGER.info("DEBUG: Storage section: {}", config.get("storage"));
            return config;
        } catch (Exception e) {
            LOGGER.error("DEBUG: Config loading failed with error: {}", e.getMessage());
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
                LOGGER.error("DEBUG: Resource not found on classpath: {}", resourcePath);
                LOGGER.error("DEBUG: Trying to load from current directory as fallback");
                // Fallback: try to load from current directory
                try {
                    return Files.newInputStream(Path.of(resourcePath));
                } catch (IOException fallbackEx) {
                    LOGGER.error("DEBUG: Fallback also failed: {}", fallbackEx.getMessage());
                    throw new IOException("Resource not found on classpath: %s, fallback also failed: %s".formatted(resourcePath, fallbackEx.getMessage()));
                }
            }
            LOGGER.info("DEBUG: Successfully loaded from classpath");
            return resourceStream;
        } else {
            LOGGER.info("AppState loading the configuration from a file");
            LOGGER.info("DEBUG: File path: {}", path);
            return Files.newInputStream(Path.of(path));
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


}
