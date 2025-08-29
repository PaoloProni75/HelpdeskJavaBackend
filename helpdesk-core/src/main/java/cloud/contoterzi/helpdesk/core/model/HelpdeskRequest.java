package cloud.contoterzi.helpdesk.core.model;

/**
 * Helpdesk request model.
 * This class is used to represent a request to the helpdesk system.
 * That means that it is not a request to the LLM, but rather a request
 * to the helpdesk system itself.
 */
public class HelpdeskRequest {
    private String question;

    public HelpdeskRequest() {}

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}