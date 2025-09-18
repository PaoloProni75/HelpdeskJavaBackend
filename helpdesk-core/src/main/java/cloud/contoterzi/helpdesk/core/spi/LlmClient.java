package cloud.contoterzi.helpdesk.core.spi;

import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import cloud.contoterzi.helpdesk.core.config.YamlConfig;

/**
 * Interface for LLM client providers.
 * Implementations of this interface must provide a method to generate an answer.
 * The implementor represents directly an LLM service to which the system will communicate.
 */
public interface LlmClient {
    /**
     * Short identifier for this provider, e.g. "anthropic", "openai-azure", "nova".
     */
    String id();

    /**
     * Initialize with the config subtree; called once.
     * @param config The configuration subtree for this provider.
     */
    void init(YamlConfig config);

    /**
     * Generate answer given prompt and optional system/instructions.
     * @param request The request to the LLM.
     * @return Normalized response from the LLM.
     * @throws LlmException If there is an error (invalid input, auth, provider, ecc.)
     */
    LlmResponse ask(LlmRequest request) throws LlmException;
}