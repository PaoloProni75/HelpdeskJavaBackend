package cloud.contoterzi.aws.common;

import cloud.contoterzi.helpdesk.core.engine.HelpdeskEngine;
import cloud.contoterzi.helpdesk.core.handler.HandlerConstants;
import cloud.contoterzi.helpdesk.core.model.HelpdeskRequest;
import cloud.contoterzi.helpdesk.core.model.HelpdeskResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.IOException;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.locks.ReentrantLock;


public class LambdaHandler implements RequestHandler<Map<String, Object>, HelpdeskResponse>, HandlerConstants {
    private static final Logger LOGGER = Logger.getLogger(LambdaHandler.class.getName());

    // Static engine - initialized only once
    private static HelpdeskEngine engine;
    private static final ReentrantLock ENGINE_LOCK = new ReentrantLock();

    // TODO: take away this static part as it is in the SuperHandler class
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
        ENGINE_LOCK.lock();
        try {
            if (engine == null) {
                engine = new HelpdeskEngine();
                engine.init();
                LOGGER.info("Static engine initialized");
            }
        }
        finally {
            ENGINE_LOCK.unlock();
        }
    }

    public LambdaHandler() {
        // Lazy initialization will happen on first request
    }

    @Override
    public HelpdeskResponse handleRequest(Map<String, Object> event, Context context) {
        final String reqId = (context != null) ? context.getAwsRequestId() : "n/a";
        // Handle warmup requests
        if (event != null && WARMUP_SOURCE.equals(event.get("source"))) {
            LOGGER.log(Level.INFO, () -> String.format(LOG_WARMUP_REQUEST, reqId));
            return HelpdeskResponse.builder()
                    .answer(MSG_WARMED)
                    .source(SOURCE_SYSTEM)
                    .action(ACTION_NONE)
                    .escalation(false)
                    .confidence(1.0)
                    .responseTimeMs(0)
                    .build();
        }

        final String qRaw = (event != null) ? asString(event.get(KEY_QUESTION)) : null;
        final String q = (qRaw != null) ? qRaw.trim() : null;

        LOGGER.log(Level.INFO, () -> String.format(
                LOG_REQUEST_START + " keys=%s",
                reqId,
                (q != null ? q.length() : 0),
                q,
                (event != null ? event.keySet() : java.util.Collections.emptySet())
        ));

        if (q == null || q.isBlank()) {
            LOGGER.log(Level.WARNING, () -> String.format(LOG_MISSING_QUESTION, reqId));
            return HelpdeskResponse.builder()
                    .answer(MSG_MISSING_QUESTION)
                    .source(SOURCE_SYSTEM)
                    .action(ACTION_NONE)
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
                    LOG_REQUEST_SUCCESS,
                    reqId,
                    response.getResponseTimeMs(),
                    response.isEscalation(),
                    response.getConfidence(),
                    response.getSource()
            ));
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(LOG_REQUEST_ERROR, reqId), e);
            return HelpdeskResponse.builder()
                    .answer(MSG_INTERNAL_ERROR)
                    .source(SOURCE_SYSTEM)
                    .action(ACTION_NONE)
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