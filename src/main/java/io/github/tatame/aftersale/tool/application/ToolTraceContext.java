package io.github.tatame.aftersale.tool.application;

import java.util.Optional;

public final class ToolTraceContext {

    private static final ThreadLocal<String> CURRENT_RUN_ID = new ThreadLocal<>();

    private ToolTraceContext() {
    }

    public static Optional<String> currentRunId() {
        return Optional.ofNullable(CURRENT_RUN_ID.get());
    }

    public static void runWith(String runId, Runnable action) {
        String previousRunId = CURRENT_RUN_ID.get();
        CURRENT_RUN_ID.set(runId);
        try {
            action.run();
        } finally {
            if (previousRunId == null) {
                CURRENT_RUN_ID.remove();
            } else {
                CURRENT_RUN_ID.set(previousRunId);
            }
        }
    }
}
