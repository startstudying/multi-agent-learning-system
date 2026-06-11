package com.learningos.health.api;

import java.util.Map;

public final class HealthDtos {

    private HealthDtos() {
    }

    public record HealthResponse(
            ComponentStatus application,
            ComponentStatus database,
            ComponentStatus redis,
            ComponentStatus minio,
            ComponentStatus model,
            ComponentStatus vector
    ) {
    }

    public record ComponentStatus(
            String status,
            String detail,
            Map<String, Object> metadata
    ) {
        public static ComponentStatus up(String detail, Map<String, Object> metadata) {
            return new ComponentStatus("UP", detail, metadata);
        }

        public static ComponentStatus configured(String detail, Map<String, Object> metadata) {
            return new ComponentStatus("CONFIGURED", detail, metadata);
        }

        public static ComponentStatus unconfigured(String detail, Map<String, Object> metadata) {
            return new ComponentStatus("UNCONFIGURED", detail, metadata);
        }

        public static ComponentStatus down(String detail, Map<String, Object> metadata) {
            return new ComponentStatus("DOWN", detail, metadata);
        }

        public static ComponentStatus disabled(String detail, Map<String, Object> metadata) {
            return new ComponentStatus("DISABLED", detail, metadata);
        }
    }
}
