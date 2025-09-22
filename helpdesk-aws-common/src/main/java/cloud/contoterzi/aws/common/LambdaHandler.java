package cloud.contoterzi.aws.common;

import cloud.contoterzi.helpdesk.core.engine.HelpdeskEngine;
import cloud.contoterzi.helpdesk.core.model.HelpdeskRequest;
import cloud.contoterzi.helpdesk.core.model.HelpdeskResponse;
import cloud.contoterzi.helpdesk.core.handler.SuperHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class LambdaHandler extends SuperHandler implements RequestHandler<Map<String, Object>, HelpdeskResponse> {

    // Static engine - initialized only once
    private static HelpdeskEngine engine;

    public LambdaHandler() {
    }

    @Override
    public HelpdeskResponse handleRequest(Map<String, Object> event, Context context) {
        final String reqId = (context != null) ? context.getAwsRequestId() : "n/a";
        // Handle warmup requests
        if (event != null && WARMUP_SOURCE.equals(event.get("source"))) {
            LOGGER.info(String.format(LOG_WARMUP_REQUEST, reqId));
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

        LOGGER.info(String.format(
                LOG_REQUEST_START + " keys=%s",
                reqId,
                (q != null ? q.length() : 0),
                q,
                (event != null ? event.keySet() : java.util.Collections.emptySet())
        ));

        if (q == null || q.isBlank()) {
            LOGGER.warn(String.format(LOG_MISSING_QUESTION, reqId));
            return RESPONSE_FOR_MISSING_QUESTION;
        }

        try {
            engine = getOrInitializeEngine(); // thread safe

            HelpdeskRequest request = new HelpdeskRequest();
            request.setQuestion(q);

            // Here calls the LLM
            LOGGER.warn("Just before asking");
            HelpdeskResponse response = engine.processQuestion(request);
            LOGGER.warn("Just after asking");

            LOGGER.warn(String.format(
                    LOG_REQUEST_SUCCESS,
                    reqId,
                    response.getResponseTimeMs(),
                    response.isEscalation(),
                    response.getConfidence(),
                    response.getSource()
            ));
            return response;

        } catch (Exception e) {
            LOGGER.error(String.format(LOG_REQUEST_ERROR, reqId), e);
            return RESPONSE_FOR_MISSING_QUESTION;
        }
    }


    // --- helper for safe conversion ---
    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}