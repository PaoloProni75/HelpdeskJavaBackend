package cloud.contoterzi.helpdesk.core.engine;

import cloud.contoterzi.helpdesk.core.config.AppState;
import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.*;
import cloud.contoterzi.helpdesk.core.spi.LlmClient;
import cloud.contoterzi.helpdesk.core.spi.SimilarityService;
import cloud.contoterzi.helpdesk.core.util.SpiLoader;
import cloud.contoterzi.helpdesk.core.handler.SuperHandler;

import java.io.IOException;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The HelpdeskEngine class is responsible for processing user queries in a helpdesk system.
 * It integrates multiple services, including a knowledge base, similarity matching,
 * and a large language model (LLM), to generate appropriate responses.
 * The class can determine whether a query requires human intervention or can be handled by the system.
 */
public class HelpdeskEngine {
    private static final Logger LOGGER = Logger.getLogger(HelpdeskEngine.class.getName());

    private static final String ACTION_NOTIFY_HUMAN = "notify_human";
    private static final String ACTION_NONE = "none";
    public static final String KB = "kb";
    public static final String LLM = "llm";

    public static final String FALLBACK =
            "Sorry, I don't know the answer to that question.";

    /**
     * Sentinel sentence to detect the need of escalation to a human support in the LLM response.
     * This will be configured from the config file.
     */
    private String contactSupportPhrase = "contact support"; // Default fallback

    private List<IKnowledge> kb;
    private LlmClient llm;
    private SimilarityService similarityService;
    private double threshold;
    private YamlConfig config; // Store config to access prompts

    public HelpdeskEngine() {}

    /**
     * This method initializes the engine.
     * As it uses some slow I/O operations, it contains initialization logic
     * instead of the constructor.
     */
    public void init() throws IOException {
        // This block is executed the first time only when the method is called.
        // Next calls never enter here.
        LOGGER.info("Initializing Helpdesk Engine");
        AppState state = AppState.INSTANCE;
        LOGGER.info("About to initialize AppState");
        state.init();
        LOGGER.info("AppState initialized successfully");
        YamlConfig cfg = state.getConfiguration();
        assert cfg != null;
        this.config = cfg; // Store config
        this.kb = state.getKnowledgeBase();
        assert kb != null;
        LOGGER.info("Knowledge base loaded with " + kb.size() + " entries");
        String llmType = cfg.getString("llm.type");
        assert llmType != null;
        LOGGER.info("Loading LLM client type: " + llmType);
        this.llm = SpiLoader.loadByKey(LlmClient.class, llmType);
        if (this.llm == null) {
            LOGGER.severe("LLM client not found for type: " + llmType);
            throw new IOException("Cannot load LLM client for type: " + llmType);
        }
        LOGGER.info("LLM client loaded successfully: " + this.llm.getClass().getName());
        this.llm.init(cfg);
        LOGGER.info("LLM client initialized successfully");
        String similarityType = cfg.getString("similarity.type", "cosine");
        this.similarityService = SpiLoader.loadByKey(SimilarityService.class, similarityType);
        if (similarityService == null)
            LOGGER.severe("Similarity Service not LOADED via SPI");
        else
            LOGGER.info("Similarity Service loaded via SPI successfully");
        this.similarityService.init(cfg);
        this.threshold = cfg.getDouble("similarity.threshold", 0.8);
        // Initialize contactSupportPhrase from config
        this.contactSupportPhrase = cfg.getString("llm.prompts.contactSupportPhrase", "contact support");
    }

    /**
     * Processes a helpdesk question by determining the best response strategy.
     * It leverages a similarity service to find the best matching knowledge base item
     * and determines whether the request should escalate to human intervention or invoke
     * an LLM (Large Language Model).
     *
     * @param request The helpdesk request that contains the question to process.
     * @return A {@code HelpdeskResponse} representing the system's response, including
     *         confidence score, escalation status, and suggested actions.
     */
    public HelpdeskResponse processQuestion(final HelpdeskRequest request) throws IOException {
        if (this.llm == null)
            throw new IllegalStateException("Helpdesk Engine not initialized. Did you call init()?");

        final KnowledgeBestMatch bestMatch = this.similarityService.findBestMatch(request.getQuestion(), kb, threshold);
        final IKnowledge bestItem = bestMatch.getBestKBItem();
        final boolean hasBest = bestItem != null;
        final boolean shouldEscalate = !hasBest || bestItem.isEscalation();
        final double confidence = bestMatch.getBestSim();
        final String action = shouldEscalate ? ACTION_NOTIFY_HUMAN : ACTION_NONE;

        final HelpdeskResponse.Builder builder = HelpdeskResponse.builder();
        builder .confidence(confidence)
                .action(action);
        if (AppState.INSTANCE.isAlwaysCallLlm() || bestMatch.isShouldInvokeLlm())
            handleLlmPath(builder, request);
        else
             // The request goes directly to the Knowledg base
            handleKbPath(builder, bestItem);

        return builder.build();
    }

    private void handleLlmPath(HelpdeskResponse.Builder builder, HelpdeskRequest request) {
        try {
            // Construct the prompt using template and examples from knowledge base
            String prompt = buildPromptWithTemplate(request.getQuestion());
            
            // HERE IS THE CALL TO THE 'DRIVER' FOR THE Long Language Model
            final LlmResponse llmResponse = llm.ask(new LlmRequest(prompt));

            final String answer = (llmResponse != null) ? llmResponse.getAnswer() : null;
            final boolean escalation = containsContactSupport(answer);

            builder.answer(answer == null ? FALLBACK : answer)
                    .escalation(escalation)
                    .source(LLM)
                    .responseTimeMs(llmResponse != null ? llmResponse.getTimeMs() : 0L);
        } catch (LlmException ex) {
            // In case of LLM error: save fallback and possible escalation
            final boolean escalation = ex.isNotRetryable();
            builder.answer(FALLBACK)
                    .confidence(0)
                    .escalation(escalation)
                    .source(LLM)
                    .responseTimeMs(0L);
            LOGGER.log(Level.WARNING, "LLM error", ex);
        }
    }

    private void handleKbPath(HelpdeskResponse.Builder builder, IKnowledge bestItem) {
        final boolean hasBest = bestItem != null;
        final boolean escalation = !hasBest || bestItem.isEscalation();
        builder.answer(hasBest ? bestItem.getAnswer() : FALLBACK)
                .escalation(escalation)
                .source(KB)
                .responseTimeMs(0L);
    }

    private boolean containsContactSupport(String answer) {
        return answer != null && answer.toLowerCase().contains(contactSupportPhrase.toLowerCase());
    }
    
    /**
     * Builds the prompt using the configured template with examples from the knowledge base.
     * Template format: preamble + "\n\nExamples:\n" + examples + "\n\nUser question: " + question + "\nAnswer:"
     */
    private String buildPromptWithTemplate(String userQuestion) {
        String preamble = config.getString("llm.prompts.preamble");
        String template = config.getString("llm.prompts.template");

        if (template == null || template.isEmpty()) {
            // Fallback to simple question if no template
            return userQuestion;
        }
        
        // Build examples from knowledge base (limit to first 10 for brevity)
        StringBuilder examples = new StringBuilder();
        int count = 0;
        for (IKnowledge entry : kb) {
            if (count >= 10) break;
            examples.append("- ").append(entry.getQuestion())
                   .append(" -> ").append(entry.getAnswer()).append("\n");
            count++;
        }
        
        // Apply template: template should contain %s placeholders for preamble, examples, question
        try {
            return String.format(template, 
                preamble != null ? preamble : "",
                examples.toString().trim(),
                userQuestion);
        } catch (Exception e) {
            LOGGER.warning("Error formatting template, falling back to simple question: " + e.getMessage());
            return userQuestion;
        }
    }
}
