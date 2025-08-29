package cloud.contoterzi.helpdesk.core.model;

/**
 * Represents a result model for determining the best matching knowledge base entry
 * based on a similarity score. Additionally, it indicates whether a large language
 * model (LLM) invocation is necessary.
 */
public class KnowledgeBestMatch {
    /**
     * Indicates whether a large language model (LLM) invocation is necessary.
     */
    private boolean shouldInvokeLlm;

    /**
     * The similarity score of the best match.
     */
    private double bestSim;

    /**
     * The best matching knowledge base item
     */
    private IKnowledge bestKBItem;

    /**
     * Default constructor.
     * @param shouldInvokeLlm true if LLM should be invoked, false otherwise.
     * @param bestSim the similarity score of the best match.
     * @param bestKBItem the best matching knowledge base item.
     */
    public KnowledgeBestMatch(boolean shouldInvokeLlm, double bestSim, IKnowledge bestKBItem) {
        this.shouldInvokeLlm = shouldInvokeLlm;
        this.bestSim = bestSim;
        this.bestKBItem = bestKBItem;
    }

    /**
     * Indicates whether a large language model (LLM) invocation is necessary.
     * @return true if LLM should be invoked, false otherwise.
     */
    public boolean isShouldInvokeLlm() {
        return shouldInvokeLlm;
    }

    /**
     * Gets the similarity score of the best match.
     * @return the similarity score.
     */
    public double getBestSim() {
        return bestSim;
    }

    /**
     * Gets the best matching knowledge base item.
     * @return the best matching knowledge base item.
     */
    public IKnowledge getBestKBItem() {
        return bestKBItem;
    }
}
