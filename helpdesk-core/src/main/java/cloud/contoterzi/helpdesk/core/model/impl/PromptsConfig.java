package cloud.contoterzi.helpdesk.core.model.impl;

import cloud.contoterzi.helpdesk.core.model.IPromptConfig;

/**
 * Configuration for prompts.
 */
public class PromptsConfig  implements IPromptConfig {
    /**
     * Preamble text to be included in the prompt.
     */
    private String preamble;

    /**
     * Template for the prompt.
     */
    private String template;

    /**
     * Contact support phrase.
     */
    private String contactSupportPhrase;

    public PromptsConfig() {}

    public String getPreamble() {
        return preamble;
    }

    public void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getContactSupportPhrase() {
        return contactSupportPhrase;
    }

    public void setContactSupportPhrase(String contactSupportPhrase) {
        this.contactSupportPhrase = contactSupportPhrase;
    }
}
