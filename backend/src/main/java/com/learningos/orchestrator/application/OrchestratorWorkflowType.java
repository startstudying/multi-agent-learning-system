package com.learningos.orchestrator.application;

import com.learningos.agent.application.AgentRuntimeConstants;
import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;

import java.util.Locale;

enum OrchestratorWorkflowType {
    LEARNING_GOAL_CREATION("LEARNING_GOAL_CREATION"),
    RAG_QA("RAG_QA"),
    RESOURCE_GENERATION(AgentRuntimeConstants.TASK_TYPE_RESOURCE_GENERATION),
    ANSWER_SUBMISSION("ANSWER_SUBMISSION");

    private final String value;

    OrchestratorWorkflowType(String value) {
        this.value = value;
    }

    static OrchestratorWorkflowType from(String workflowType) {
        String normalized = workflowType == null
                ? ""
                : workflowType.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LEARNING_GOAL_CREATION", "LEARNING_PATH_CREATION" -> LEARNING_GOAL_CREATION;
            case "RAG_QA", "RAG_QUESTION_ANSWERING", "COURSE_RAG_QA" -> RAG_QA;
            case "RESOURCE_GENERATION" -> RESOURCE_GENERATION;
            case "ANSWER_SUBMISSION", "ANSWER_SUBMIT" -> ANSWER_SUBMISSION;
            default -> throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported workflowType");
        };
    }

    String value() {
        return value;
    }

    String taskType() {
        return value;
    }
}
