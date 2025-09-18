package cloud.contoterzi.ibm.watsonx;

import cloud.contoterzi.helpdesk.core.config.YamlConfig;
import cloud.contoterzi.helpdesk.core.llm.AbstractLlmClient;
import cloud.contoterzi.helpdesk.core.llm.LlmException;
import cloud.contoterzi.helpdesk.core.model.LlmRequest;
import cloud.contoterzi.helpdesk.core.model.LlmResponse;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WatsonClient extends AbstractLlmClient {
    private final String WATSONX = "watsonx";
    // Configuration fields
    private String endpoint;
    private String projectId;
    private String model;
    private String version;
    private double temperature;
    private int maxTokens;
    private String systemPrompt;

    // HTTP client and authentication
    private HttpClient httpClient;
    private IamAuthenticator authenticator;
    private ObjectMapper objectMapper;

    @Override
    public boolean isTimeoutException(Throwable t) {
        // Common timeout exceptions for HTTP clients
        return t instanceof java.net.SocketTimeoutException ||
               t instanceof java.net.http.HttpTimeoutException ||
               t instanceof java.util.concurrent.TimeoutException ||
               (t.getCause() != null && isTimeoutException(t.getCause()));
    }

    /**
     * Sends the request to the LLM and returns its response.
     * @param request the LLM request containing the prompt and other parameters
     * @return LLM response
     * @throws LlmException A problem occurred during the LLM call
     */
    @Override
    protected LlmResponse callTheLLM(LlmRequest request) throws LlmException {
        LOGGER.info("=== Watson Client Call Started ===");

        if (request == null || request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            LOGGER.error("Invalid request or prompt");
            throw new IllegalArgumentException("Request and its prompt cannot be null or empty");
        }

        String prompt = request.getPrompt().trim();
        LOGGER.info("""
                Request prompt: {}
                Watson endpoint: {}
                Model: {}
                Temperature: {}
                Max tokens: {}
                """, prompt, endpoint, model, temperature,  maxTokens);

        try {
            // Build the request payload for Watson
            LOGGER.info("Building Watson request...");

            // Create the complete prompt with system context
            String fullPrompt = prompt;
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                fullPrompt = systemPrompt.trim() + " " + prompt;
                LOGGER.info("Added system prompt. Full prompt length: " + fullPrompt.length());
            }

            // Watson API seems to have issues with newlines in content, so replace with spaces
            fullPrompt = fullPrompt.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ").trim();
            LOGGER.info("Cleaned prompt length: " + fullPrompt.length());

            long startTime = System.currentTimeMillis();

            // Get access token from authenticator
            String accessToken = authenticator.requestToken().getAccessToken();

            // Build Watson API request payload - completely avoid any potential JSON issues
            // by manually escaping only what's necessary and avoiding Jackson complexity
            String escapedPrompt = fullPrompt
                .replace("\\", "\\\\")  // Escape backslashes first
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\b", "\\b")   // Escape backspace
                .replace("\f", "\\f")   // Escape form feed
                .replace("\r", "\\r")   // Escape carriage return
                .replace("\t", "\\t");  // Escape tab
                // Note: we already removed \n characters above

            String requestBody = String.format(java.util.Locale.US, """
                {
                    "model_id": "%s",
                    "project_id": "%s",
                    "max_new_tokens": %d,
                    "temperature": %.2f,
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ]
                }""", model.trim(), projectId.trim(), maxTokens, temperature, escapedPrompt);

            // Build HTTP request (new chat API)
            String apiUrl = endpoint + "/ml/v1/text/chat?version=" + version;
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMinutes(2))
                .build();

            LOGGER.info("=== WATSON CLIENT v2024-09-18-04:50 - NEWLINE + MODEL FIX ACTIVE ===");
            LOGGER.info("Calling Watson API at: " + apiUrl);
            LOGGER.info("Full prompt content: [{}]", fullPrompt);
            LOGGER.info("Escaped prompt content: [{}]", escapedPrompt);
            LOGGER.info("Request payload: " + requestBody);

            // Send HTTP request
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            long endTime = System.currentTimeMillis();
            LOGGER.info("API call completed in {}ms\nResponse status: {}\nResponse body: {}",
                    endTime - startTime,
                    httpResponse.statusCode(),
                    httpResponse.body());

            // Check response status
            if (httpResponse.statusCode() != 200) {
                LOGGER.warn("Watson API returned error status: {}", httpResponse.statusCode());
                throw new RuntimeException("Watson API error: " + httpResponse.statusCode() + " - " + httpResponse.body());
            }

            // Parse JSON response (new chat format)
            JsonNode responseJson = objectMapper.readTree(httpResponse.body());
            JsonNode choices = responseJson.get("choices");

            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                LOGGER.warn("Empty choices from Watson API");
                throw new RuntimeException("Empty choices from Watson API");
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.get("message");
            JsonNode contentNode = message != null ? message.get("content") : null;

            if (contentNode == null) {
                LOGGER.warn("No content in Watson response message");
                throw new RuntimeException("No content in Watson response message");
            }

            String responseText = contentNode.asText();
            if (responseText == null || responseText.trim().isEmpty()) {
                LOGGER.warn("Empty generated text from Watson");
                throw new RuntimeException("Empty generated text from Watson");
            }

            LlmResponse llmResponse = new LlmResponse(responseText.trim());
            LOGGER.info("Watson Client Call Completed Successfully");
            return llmResponse;

        }
        catch (Exception ex) {
            LOGGER.error("Watson API Call Failed ===\nException type: {}\nException message: {}",
                    ex.getClass().getName(),
                    ex.getMessage(), ex);

            if (ex.getCause() != null)
                LOGGER.error("Root cause: {} - {}", ex.getCause().getClass().getName(), ex.getCause().getMessage());
            throw new RuntimeException("IBM Watsonx API call failed {}", ex);
        }

    }

    @Override
    public String id() {
        return WATSONX;
    }

    /**
     * Initialize the communication with IBM Watsonx.
     * It stores the instance variables that are going to be used in the callTheLLM method.
     * @param config The configuration subtree for this provider.
     */
    @Override
    public void init(YamlConfig config) {
        LOGGER.info("=== Initializing Watson Client ===");

        // Extract Watson-specific configuration
        this.endpoint = config.getString("llm.watson.endpoint");
        String apiKey = config.getString("llm.watson.apiKey");
        this.projectId = config.getString("llm.watson.projectId");
        this.model = config.getString("llm.watson.model");
        this.version = config.getString("llm.watson.version");

        // Extract general LLM configuration
        this.temperature = config.getDouble("llm.temperature", 0.7);
        this.maxTokens = config.getInt("llm.maxTokens", 1024);
        this.systemPrompt = config.getString("llm.prompts.preamble");

        // Log configuration (without sensitive data)
        LOGGER.info("""
                        Configuration loaded:
                          Watson endpoint: {}
                          Project ID: {}
                          Model: {}
                          Version: {}
                          API key provided: {}
                          Temperature: {}
                          Max tokens: {}
                          System prompt length: {}
                        """,endpoint, projectId, model, version,
                (apiKey != null && !apiKey.trim().isEmpty()), temperature, maxTokens,
                (systemPrompt != null ? systemPrompt.length() : 0));

        // Validate required configuration
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Watson endpoint cannot be null or empty");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Watson API key cannot be null or empty");
        }
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("Watson project ID cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Watson model cannot be null or empty");
        }

        // Initialize HTTP client and authenticator
        try {
            this.authenticator = new IamAuthenticator.Builder()
                .apikey(apiKey)
                .build();

            this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

            this.objectMapper = new ObjectMapper();

            LOGGER.info("Watson HTTP client initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Watson HTTP client: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Watson HTTP client", e);
        }

        LOGGER.info("=== Watson Client Initialized Successfully ===");
    }
}