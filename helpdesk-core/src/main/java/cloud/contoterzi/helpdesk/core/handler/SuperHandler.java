package cloud.contoterzi.helpdesk.core.handler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;
import java.util.logging.Logger;

import cloud.contoterzi.helpdesk.core.engine.HelpdeskEngine;

public class SuperHandler implements HandlerConstants {
    protected static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static final ReentrantLock ENGINE_LOCK = new ReentrantLock();
    public static final String INITIALIZING_ENGINE_ON_FIRST_REQUEST = "Initializing engine on first request";
    public static final String ENGINE_INITIALIZED_SUCCESSFULLY = "Engine initialized successfully";
    public static final String WITHIN_30_SECONDS_POTENTIAL_DEADLOCK = "Could not acquire engine lock within 30 seconds - potential deadlock";
    public static final String ENGINE_INITIALIZATION_WAS_INTERRUPTED = "Engine initialization was interrupted";
    protected HelpdeskEngine engine = null;

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
