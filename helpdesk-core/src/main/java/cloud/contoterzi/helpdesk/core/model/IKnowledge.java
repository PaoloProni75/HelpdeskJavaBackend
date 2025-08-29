package cloud.contoterzi.helpdesk.core.model;

/**
 * Knowledge entry model.
 */
public interface IKnowledge {
    int getId();

    String getQuestion();

    String getAnswer();

    boolean isEscalation();
}
