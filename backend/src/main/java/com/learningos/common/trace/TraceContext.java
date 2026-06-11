package com.learningos.common.trace;

import java.util.Optional;

public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(TRACE_ID_HOLDER.get());
    }

    public static void setCurrentTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            TRACE_ID_HOLDER.remove();
            return;
        }
        TRACE_ID_HOLDER.set(traceId);
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
