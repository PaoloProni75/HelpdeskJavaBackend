package cloud.contoterzi.helpdesk.core.model.impl;

import cloud.contoterzi.helpdesk.core.model.IKnowledge;

/**
 * Knowledge entry model.
 */
public class KnowledgeEntry implements IKnowledge {
    private int id;
    private String question;
    private String answer;
    private Boolean escalation;

    public KnowledgeEntry() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Boolean getEscalation() {
        return escalation;
    }

    public void setEscalation(Boolean escalation) {
        this.escalation = escalation;
    }

    @Override
    public boolean isEscalation() {
        return escalation != null && escalation;
    }
}