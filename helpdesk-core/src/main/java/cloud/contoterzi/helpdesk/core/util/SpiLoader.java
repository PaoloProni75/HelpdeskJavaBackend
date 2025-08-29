package cloud.contoterzi.helpdesk.core.util;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Utility class for loading SPI implementations by ServiceLoader,
 * filtering by a 'key' which is declared by the provider.
 */
public class SpiLoader {
    private static final Logger LOGGER = Logger.getLogger(SpiLoader.class.getName());

    private static final String IMPLEMENTATION_ERROR_MESSAGE = "The implementation %s does not expose nor 'id()' nor 'type()'";
    private static final String ERROR_READING_KEY_MESSAGE = "Error reading the key from %s";
    private static final String NO_SPI_IMPLEMENTATION_MESSAGE = "No SPI implementation has been found for %s with key %s";
    private static final String[] KEY_METHOD_CANDIDATES = {"id", "type"};

    private SpiLoader() {
    }

    /**
     * Loads the implementation of an SPI interface which is identified by the requested 'key'.
     *
     * @param api       The SPI interface (e.g., StorageAdapter.class)
     * @param wantedKey The key to compare (e.g., "s3", "fs", "oss")
     * @return The matching instance, if found
     * @throws IllegalStateException if no implementation is compatible.
     */
    public static <T> T loadByKey(Class<T> api, String wantedKey) {
        ServiceLoader<T> loader = ServiceLoader.load(api);
        for (T impl : loader) {
            String key = extractKey(impl);
            if (wantedKey.equalsIgnoreCase(key)) {
                return impl;
            }
        }
        throw new NoSuchElementException(NO_SPI_IMPLEMENTATION_MESSAGE.formatted(api.getSimpleName(), wantedKey));
    }

    /**
     * Tries to invoke a public method "id()" or "type()" on the given implementation.
     * @param impl The implementation to extract the key from.
     * @return The key extracted from the implementation.
     * @throws IllegalStateException if the implementation does not expose an "id()" or "type()" method.
     */
    private static String extractKey(Object impl) {
        Class<?> clazz = impl.getClass();
        for (String methodName : KEY_METHOD_CANDIDATES) { // id, type
            try {
                return (String) clazz.getMethod(methodName).invoke(impl);
            } catch (NoSuchMethodException ignore) {
                LOGGER.fine("%s not found, trying next candidate".formatted(methodName));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException(IMPLEMENTATION_ERROR_MESSAGE.formatted(impl.getClass().getName()), ex);
            }
        }
        throw new IllegalStateException(IMPLEMENTATION_ERROR_MESSAGE.formatted(clazz.getName()));
    }
}