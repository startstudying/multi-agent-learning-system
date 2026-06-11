package com.learningos.agent.application;

public final class AgentRuntimeConstants {

    public static final String TASK_TYPE_RESOURCE_GENERATION = "RESOURCE_GENERATION";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_WAITING_REVIEW = "WAITING_REVIEW";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final java.util.Set<String> TASK_STATUSES = java.util.Set.of(
            STATUS_PENDING,
            STATUS_RUNNING,
            STATUS_WAITING_REVIEW,
            STATUS_DONE,
            STATUS_FAILED,
            STATUS_CANCELLED
    );

    public static final java.util.Set<String> TRACE_STATUSES = java.util.Set.of(
            STATUS_PENDING,
            STATUS_RUNNING,
            STATUS_WAITING,
            STATUS_WAITING_REVIEW,
            STATUS_DONE,
            STATUS_FAILED,
            STATUS_CANCELLED
    );

    public static final java.util.Set<String> TERMINAL_STATUSES = java.util.Set.of(
            STATUS_DONE,
            STATUS_FAILED,
            STATUS_CANCELLED
    );

    public static final java.util.Set<String> CANCELLABLE_TASK_STATUSES = java.util.Set.of(
            STATUS_PENDING,
            STATUS_RUNNING,
            STATUS_WAITING_REVIEW
    );

    public static final String REVIEW_PENDING_CRITIC = "PENDING_CRITIC";
    public static final String REVIEW_DRAFT = "DRAFT";
    public static final String REVIEW_APPROVED = "APPROVED";
    public static final String REVIEW_REVISION_REQUESTED = "REVISION_REQUESTED";
    public static final String REVIEW_REJECTED = "REJECTED";
    public static final String REVIEW_PUBLISHED = "PUBLISHED";

    public static final java.util.Set<String> REVIEW_STATUSES = java.util.Set.of(
            REVIEW_DRAFT,
            REVIEW_PENDING_CRITIC,
            REVIEW_APPROVED,
            REVIEW_REVISION_REQUESTED,
            REVIEW_REJECTED,
            REVIEW_PUBLISHED
    );

    public static final String SAFETY_NEEDS_REVIEW = "NEEDS_REVIEW";

    public static final String MODEL_DETERMINISTIC_DEMO = "deterministic-demo-model";
    public static final String MODEL_CALL_SUCCESS = "SUCCESS";
    public static final String MODEL_CALL_FAILED = "FAILED";
    public static final String MODEL_STRUCTURED_OUTPUT_INVALID = "STRUCTURED_OUTPUT_INVALID";

    public static final String PROMPT_RESOURCE_V1 = "agent-resource-v1";
    public static final String PROMPT_CRITIC_V1 = "agent-critic-v1";
    public static final String PROMPT_TUTOR_V1 = "agent-tutor-v1";
    public static final String PROMPT_SAFETY_V1 = "agent-safety-v1";

    private AgentRuntimeConstants() {
    }
}
