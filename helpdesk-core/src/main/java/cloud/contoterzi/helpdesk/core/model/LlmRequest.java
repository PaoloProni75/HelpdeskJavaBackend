package cloud.contoterzi.helpdesk.core.model;

/**
 * Generic request to a LLM.
 * Extend this class to add your own optional fields.
 */
public class LlmRequest extends LlmBaseFields {

    public LlmRequest() {
        super("");
    }

    public LlmRequest(String prompt) {
        super(prompt);
    }

    public String getPrompt() {
        return text;
    }

    // Future fields: String system, List<String> stop, Integer maxTokens, Double temperature, Map<String,Object> meta
}
