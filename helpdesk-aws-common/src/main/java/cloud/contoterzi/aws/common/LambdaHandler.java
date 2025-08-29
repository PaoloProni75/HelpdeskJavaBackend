package cloud.contoterzi.aws.common;

import cloud.contoterzi.helpdesk.core.engine.HelpdeskEngine;
import cloud.contoterzi.helpdesk.core.model.HelpdeskRequest;
import cloud.contoterzi.helpdesk.core.model.HelpdeskResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.IOException;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

public class LambdaHandler implements RequestHandler<Map<String, Object>, HelpdeskResponse> {
    private static final Logger LOGGER = Logger.getLogger(LambdaHandler.class.getName());

    // Static engine - initialized only once
    private static HelpdeskEngine engine;
    private static final Object ENGINE_LOCK = new Object();

    static {
        // initialization during class loading
        try {
            initializeEngine();
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize engine during class loading", ex);
        }
    }

    private static void initializeEngine() throws IOException {
        synchronized (ENGINE_LOCK) {
            if (engine == null) {
                engine = new HelpdeskEngine();
                engine.init();
                LOGGER.info("Static engine initialized");
            }
        }
    }

    // Request keys
    private static final String KEY_QUESTION = "question";

    // Messages
    private static final String MSG_MISSING_QUESTION = "Missing question";

    public LambdaHandler() {
        // Lazy initialization will happen on first request
    }

    @Override
    public HelpdeskResponse handleRequest(Map<String, Object> event, Context context) {
        final String reqId = (context != null) ? context.getAwsRequestId() : "n/a";
        // Handle warmup requests
        if (event != null && "warmup".equals(event.get("source"))) {
            LOGGER.log(Level.INFO, () -> String.format("Warmup request reqId=%s", reqId));
            return HelpdeskResponse.builder()
                    .answer("warmed")
                    .source("system")
                    .action("none")
                    .escalation(false)
                    .confidence(1.0)
                    .responseTimeMs(0)
                    .build();
        }

        final String qRaw = (event != null) ? asString(event.get(KEY_QUESTION)) : null;
        final String q = (qRaw != null) ? qRaw.trim() : null;

        LOGGER.log(Level.INFO, () -> String.format(
                "handleRequest start reqId=%s keys=%s question.len=%d question=\"%s\"",
                reqId,
                (event != null ? event.keySet() : java.util.Collections.emptySet()),
                (q != null ? q.length() : 0),
                q
        ));

        if (q == null || q.isBlank()) {
            LOGGER.log(Level.WARNING, () -> String.format("reqId=%s missing question", reqId));
            return HelpdeskResponse.builder()
                    .answer(MSG_MISSING_QUESTION)
                    .source("system")
                    .action("none")
                    .escalation(false)
                    .confidence(0.0)
                    .responseTimeMs(0)
                    .build();
        }

        try {
            // static engine, no initialization needed
            if (engine == null) {
                initializeEngine();  // safety fallback
            }

            HelpdeskRequest request = new HelpdeskRequest();
            request.setQuestion(q);

            // Here calls the LLM
            LOGGER.warning("Just before asking");
            HelpdeskResponse response = engine.processQuestion(request);
            LOGGER.warning("Just after asking");

            LOGGER.log(Level.INFO, () -> String.format(
                    "handleRequest ok reqId=%s ms=%d esc=%s conf=%.3f source=%s",
                    reqId,
                    response.getResponseTimeMs(),
                    response.isEscalation(),
                    response.getConfidence(),
                    response.getSource()
            ));
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Error processing request reqId=%s", reqId), e);
            return HelpdeskResponse.builder()
                    .answer("Internal error")
                    .source("system")
                    .action("none")
                    .escalation(true) // Conservative: escalate on errors
                    .confidence(0.0)
                    .responseTimeMs(0)
                    .build();
        }
    }


    // --- helper for safe conversion ---
    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}