package cloud.contoterzi.helpdesk.core.handler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.contoterzi.helpdesk.core.engine.HelpdeskEngine;
import cloud.contoterzi.helpdesk.core.model.HelpdeskResponse;

public class SuperHandler implements HandlerConstants {
    protected static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static final ReentrantLock ENGINE_LOCK = new ReentrantLock();

    protected HelpdeskEngine engine = null;

    protected final HelpdeskResponse RESPONSE_FOR_MISSING_QUESTION = HelpdeskResponse.builder()
            .answer(MSG_MISSING_QUESTION)
            .source(SOURCE_SYSTEM)
            .action(ACTION_NONE)
            .escalation(false)
            .confidence(0.0)
            .responseTimeMs(0)
            .build();

    protected final HelpdeskResponse RESPONSE_FOR_GENERIC_FAILURE = HelpdeskResponse.builder()
            .answer(MSG_INTERNAL_ERROR)
            .source(SOURCE_SYSTEM)
            .action(ACTION_NONE)
            .escalation(true) // Conservative: escalate on errors
            .confidence(0.0)
            .responseTimeMs(0)
            .build();

    protected HelpdeskEngine getOrInitializeEngine() throws IOException {
        // Double-checked locking pattern with timeout
        if (engine == null) {
            try {
                if (ENGINE_LOCK.tryLock(30, TimeUnit.SECONDS)) {
                    try {
                        if (engine == null) {
                            LOGGER.info(INITIALIZING_ENGINE_ON_FIRST_REQUEST);
                            HelpdeskEngine newEngine = new HelpdeskEngine();
                            newEngine.init();
                            engine = newEngine;
                            LOGGER.info(ENGINE_INITIALIZED_SUCCESSFULLY);
                        }
                    } finally {
                        ENGINE_LOCK.unlock();
                    }
                } else {
                    throw new IOException(WITHIN_30_SECONDS_POTENTIAL_DEADLOCK);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException(ENGINE_INITIALIZATION_WAS_INTERRUPTED, ex);
            }
        }
        return engine;
    }
}
