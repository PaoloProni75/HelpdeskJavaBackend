package cloud.contoterzi.helpdesk.core.config;

import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;

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
import org.yaml.snakeyaml.Yaml;


public enum AppState {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(AppState.class.getName());
    private static final String ALWAYS_CALL_LLM_ENV_VAR = "ALWAYS_CALL_LLM";
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private AppConfig config;
    private static List<IKnowledge> knowledge;
    private boolean alwaysCallLlm;

    public void init() throws IOException {
        if (initialized.compareAndSet(false, true)) {
            LOGGER.info("Initializing AppState");
            this.config = loadConfigFromEnv();
            final StorageAdapterProvider provider = SpiLoader.loadByKey(StorageAdapterProvider.class, config.getStorage().getType());
            final StorageAdapter storage = provider.create(buildStorageUri(config), buildStorageOptions(config));
            storageInit(storage, config);
            knowledge = safeLoadKnownledgeBase(storage);
            final String env = System.getenv(ALWAYS_CALL_LLM_ENV_VAR);
            this.alwaysCallLlm = env != null && env.equalsIgnoreCase("true");
            LOGGER.info("AppState initialized");
        }
    }

    private static AppConfig loadConfigFromEnv() {
        /*
        System.out.println("DEBUG: APP_CONFIG_PATH = " + System.getenv("APP_CONFIG_PATH"));
        System.out.println("DEBUG: File exists = " + new File("/opt/config/helpdesk-config.yaml").exists());
        System.out.println("DEBUG: File readable = " + new File("/opt/config/helpdesk-config.yaml").canRead());
        System.out.println("DEBUG: /opt directory contents = " + Arrays.toString(new File("/opt").list()));
        System.out.println("DEBUG: /opt/config directory exists = " + new File("/opt/config").exists());
        if (new File("/opt/config").exists()) {
            System.out.println("DEBUG: /opt/config contents = " + Arrays.toString(new File("/opt/config").list()));
        }
        */
        String path = System.getenv("APP_CONFIG_PATH");
        if (path == null || path.isBlank()) throw new IllegalStateException("APP_CONFIG_PATH not set");
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            final Yaml yaml = new Yaml();
            AppConfig config = yaml.loadAs(in, AppConfig.class);
/*
            System.out.println("DEBUG: Config loaded successfully");
            System.out.println("DEBUG: LLM type = " + config.getLlm().getType());
            System.out.println("DEBUG: LLM region = " + config.getLlm().getRegion());
            System.out.println("DEBUG: LLM modelId = " + config.getLlm().getModelId());
*/
            return config;
        } catch (Exception e) {
            System.out.println("DEBUG: Config loading failed with error: " + e.getMessage());
            throw new IllegalStateException("Cannot read config: %s".formatted(path), e);
        }
    }

    private static void storageInit(StorageAdapter storage, AppConfig config) {
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
    public AppConfig getConfiguration() throws IOException {
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

    private static URI buildStorageUri(AppConfig config) {
        var storageConfig = config.getStorage();
        String bucket = storageConfig.getBucket();
        String prefix = storageConfig.getPrefix();
        String filename = storageConfig.getFilename();

        String key = (prefix != null && !prefix.trim().isEmpty())
                ? prefix.trim() + "/" + filename.trim()
                : filename.trim();

        if (key.startsWith("/")) key = key.substring(1);

        return URI.create("s3://" + bucket + "/" + key);
    }

    private static Map<String, String> buildStorageOptions(AppConfig config) {
        return Map.of("region", config.getStorage().getRegion());
    }

}
