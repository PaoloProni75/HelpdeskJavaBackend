package cloud.contoterzi.ollama;

import cloud.contoterzi.helpdesk.core.llm.AbstractLlmClient;
import cloud.contoterzi.helpdesk.core.llm.InvalidRequestException;
import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.llm.ProviderException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;
import cloud.contoterzi.helpdesk.core.model.impl.AppConfig;
import cloud.contoterzi.helpdesk.core.model.impl.LlmConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Unified Ollama client that communicates with Ollama API.
 * Supports OpenAI-compatible API format via Ollama's /v1/chat/completions endpoint.
 * Works with any model supported by Ollama (GPT4All, Mistral, Nemotron, Llama2, etc.).
 */
public class OllamaClient extends AbstractLlmClient {
    
    private static final int TIMEOUT_DURATION = 30;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_DURATION))
            .build();
    private static final String MODEL = "model";
    private static final String MAX_TOKENS = "max_tokens";
    private static final String TEMPERATURE = "temperature";
    private static final String MESSAGES = "messages";
    private static final String ROLE = "role";
    private static final String CONTENT = "content";
    private static final String SYSTEM = "system";
    private static final String USER = "user";
    private static final String CHOICES = "choices";
    private static final String NO_CHOICES_IN_RESPONSE = "No choices in response";
    private static final String MESSAGE = "message";
    private static final String EMPTY_CONTENT_IN_RESPONSE = "Empty content in response";
    private static final String PROBLEM_PARSING_RESPONSE_JSON = "Problem parsing response JSON";
    private static final String OLLAMA_ENDPOINT_URL_CANNOT_BE_NULL_OR_EMPTY = "Ollama endpoint URL cannot be null or empty";
    private static final String REQUEST_AND_ITS_PROMPT_CANNOT_BE_NULL_OR_EMPTY = "Request and its prompt cannot be null or empty";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String HTTP_D_S = "HTTP %d: %s";
    private static final String V_1_CHAT_COMPLETIONS = "/v1/chat/completions";
    private static final String PROBLEM_CONVERTING_THE_REQUEST_TO_JSON = "Problem converting the request to JSON";
    private static final String NETWORK_ERROR_COMMUNICATING_WITH_OLLAMA = "Network error communicating with Ollama";
    public static final String OLLAMA = "ollama";

    private final ObjectMapper mapper = new ObjectMapper();
    
    private String baseUrl;
    private String modelId;
    private int maxTokens;
    private double temperature;
    private String systemPrompt = "";
    
    public OllamaClient() {
        // Will be initialized in init()
    }
    
    @Override
    public String id() {
        return OLLAMA;
    }
    
    @Override
    public void init(AppConfig config) {
        LlmConfig llmConfig = config.getLlm();
        
        // Ollama endpoint configuration
        this.baseUrl = llmConfig.getEndpoint();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException(OLLAMA_ENDPOINT_URL_CANNOT_BE_NULL_OR_EMPTY);
        }
        
        // Ensure baseUrl doesn't end with slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        this.modelId = llmConfig.getModelId();
        this.maxTokens = llmConfig.getMaxTokens();
        this.temperature = llmConfig.getTemperature();
        
        // Initialize prompts configuration
        if (llmConfig.getPrompts() != null) {
            this.systemPrompt = llmConfig.getPrompts().getPreamble() != null 
                ? llmConfig.getPrompts().getPreamble() : "";
        }
    }
    
    @Override
    protected LlmResponse invokeProvider(LlmRequest request) throws LlmException {
        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new InvalidRequestException(REQUEST_AND_ITS_PROMPT_CANNOT_BE_NULL_OR_EMPTY);
        }
        
        try {
            Map<String, Object> payload = createPayload(request);
            String jsonPayload = mapper.writeValueAsString(payload);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + V_1_CHAT_COMPLETIONS))
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .header(ACCEPT, APPLICATION_JSON)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new ProviderException(String.format(HTTP_D_S,
                        response.statusCode(), response.body()));
            }
            
            return parseResponse(response.body());
            
        } catch (JsonProcessingException ex) {
            throw new InvalidRequestException(PROBLEM_CONVERTING_THE_REQUEST_TO_JSON, ex);
        } catch (IOException | InterruptedException ex) {
            throw new ProviderException(NETWORK_ERROR_COMMUNICATING_WITH_OLLAMA, ex);
        }
    }
    
    private Map<String, Object> createPayload(LlmRequest request) {
        String prompt = request.getPrompt().trim();
        
        if (!systemPrompt.isEmpty()) {
            // Include system message
            return Map.of(
                    MODEL, modelId,
                    MAX_TOKENS, maxTokens,
                    TEMPERATURE, temperature,
                    MESSAGES, List.of(
                    Map.of(
                            ROLE, SYSTEM,
                            CONTENT, systemPrompt
                    ),
                    Map.of(
                            ROLE, USER,
                            CONTENT, prompt
                    )
                )
            );
        } else {
            // No system prompt
            return Map.of(
                    MODEL, modelId,
                    MAX_TOKENS, maxTokens,
                    TEMPERATURE, temperature,
                    MESSAGES, List.of(
                    Map.of(
                            ROLE, USER,
                            CONTENT, prompt
                    )
                )
            );
        }
    }
    
    private LlmResponse parseResponse(String responseBody) throws LlmException {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            // Extract content from OpenAI-compatible response format
            JsonNode choices = root.path(CHOICES);
            if (choices.isEmpty()) {
                throw new ProviderException(NO_CHOICES_IN_RESPONSE);
            }
            
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path(MESSAGE);
            String content = message.path(CONTENT).asText();
            
            if (content == null || content.trim().isEmpty()) {
                throw new ProviderException(EMPTY_CONTENT_IN_RESPONSE);
            }
            
            return new LlmResponse(content.trim());
            
        } catch (JsonProcessingException ex) {
            throw new ProviderException(PROBLEM_PARSING_RESPONSE_JSON, ex);
        }
    }
}