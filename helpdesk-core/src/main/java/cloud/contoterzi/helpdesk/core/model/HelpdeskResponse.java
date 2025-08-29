package cloud.contoterzi.helpdesk.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helpdesk response model.
 * This class is used to represent a response from the helpdesk system.
 * That means that it is not a response from the LLM, but rather a response
 * from the helpdesk system itself.
 */
public class HelpdeskResponse {
    @JsonProperty("answer")
    private String answer;
    
    @JsonProperty("escalation")
    private boolean escalation;
    
    @JsonProperty("confidence")
    private double confidence;
    
    @JsonProperty("responseTimeMs")
    private long responseTimeMs;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("action")
    private String action;

    private HelpdeskResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final HelpdeskResponse response = new HelpdeskResponse();

        public Builder answer(String answer) {
            response.answer = answer;
            return this;
        }

        public Builder escalation(boolean escalation) {
            response.escalation = escalation;
            return this;
        }

        public Builder confidence(double confidence) {
            response.confidence = confidence;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            response.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder source(String source) {
            response.source = source;
            return this;
        }

        public Builder action(String action) {
            response.action = action;
            return this;
        }

        public HelpdeskResponse build() {
            return response;
        }
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isEscalation() {
        return escalation;
    }

    public double getConfidence() {
        return confidence;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getSource() {
        return source;
    }

    public String getAction() {
        return action;
    }
}