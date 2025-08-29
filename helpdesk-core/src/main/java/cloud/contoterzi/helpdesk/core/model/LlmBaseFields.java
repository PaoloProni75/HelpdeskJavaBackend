package cloud.contoterzi.helpdesk.core.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class LlmBaseFields {
    protected String text;

    protected LlmBaseFields(String text) {
        this.text = text;
        sanitize();
    }

    private final Map<String, Object> extraFields = new ConcurrentHashMap<>();

    /**
     * Adds an extra field to the request.
     * The extra fields can be used to include optional or custom parameters
     * that are not part of the predefined fields.
     *
     * @param key The key identifying the extra field. Must not be null.
     * @param value The value associated with the key. Must not be null.
     * @throws NullPointerException if either the key or value is null.
     */
    public void putExtraField(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        extraFields.put(key, value);
    }

    /**
     * Gets the value of an extra field.
     * @param key The key identifying the extra field.
     * @return The value associated with the key, or null if the key is not present.
     */
    public Object getExtraField(String key) {
        return extraFields.get(key);
    }

    /**
     * Gets the keys of all extra fields.
     * @return The keys of all extra fields.
     */
    public List<String> getExtraFieldKeys() {
        return List.copyOf(extraFields.keySet());
    }

    /**
     * Removes any leading or trailing whitespace characters from the `answer` field
     * if it is not null. This method modifies the `answer` field in place.
     */
    public void sanitize() {
        if (this.text != null) {
            this.text = this.text.strip();
        }
    }
}
