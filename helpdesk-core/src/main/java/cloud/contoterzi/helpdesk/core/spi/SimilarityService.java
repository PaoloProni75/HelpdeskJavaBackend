package cloud.contoterzi.helpdesk.core.spi;

import cloud.contoterzi.helpdesk.core.model.IKnowledge;
import cloud.contoterzi.helpdesk.core.model.KnowledgeBestMatch;
import cloud.contoterzi.helpdesk.core.config.YamlConfig;

import java.util.List;

/**
 * Interface representing a similarity service for performing operations related to
 * finding relevant matches in a knowledge base based on similarity scores.
 */
public interface SimilarityService {
    /**
     * Returns the unique identifier for this similarity service.
     * @return SPI key
     */
    default String id() {
        return "default";
    }

    /**
     * Initializes the similarity service with the given application configuration.
     * @param appConfig the application configuration to use for initialization
     */
    void init(YamlConfig appConfig);

    /**
     * Finds the best matching knowledge base item for the given question.
     * @param question The question to be answered.
     * @param kb The complete knowledge base.
     * @param threshold The minimum similarity score required for a match.
     * @return The result of the similarity search.
     */
    KnowledgeBestMatch findBestMatch(String question, List<IKnowledge> kb, double threshold);

    /**
     * Returns top-k entries most relevant to 'question'
     * @param question The question to be answered
     * @param kb The complete knowledge base.
     * @param topK The number of top entries to return
     * @return The top-k entries most relevant to 'question'
     */
    List<IKnowledge> topK(String question, List<IKnowledge> kb, int topK);
}
