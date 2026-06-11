package com.learningos.rag.api.dto;

import com.learningos.rag.domain.KbDocument;
import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.enums.DocumentStatus;
import com.learningos.rag.domain.enums.IndexTaskStatus;

import java.time.Instant;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentUploadResponse(
            String documentId,
            String indexTaskId,
            IndexTaskStatus status
    ) {
    }

    public record DocumentStatusResponse(
            String documentId,
            String kbId,
            String courseId,
            String chapterId,
            String name,
            DocumentStatus parseStatus,
            DocumentStatus indexStatus,
            String errorMessage,
            Integer version
    ) {
        public static DocumentStatusResponse from(KbDocument document) {
            return new DocumentStatusResponse(
                    document.getId(),
                    document.getKbId(),
                    document.getCourseId(),
                    document.getChapterId(),
                    document.getName(),
                    document.getParseStatus(),
                    document.getIndexStatus(),
                    document.getErrorMessage(),
                    document.getVersion()
            );
        }
    }

    public record IndexTaskDetailResponse(
            String taskId,
            String documentId,
            String kbId,
            IndexTaskStatus status,
            int progressPercent,
            String progressPhase,
            int retryCount,
            boolean recoverable,
            Instant nextRetryAt,
            Instant heartbeatAt,
            Instant leaseUntil,
            Instant startedAt,
            Instant finishedAt,
            String errorMessage
    ) {
        public static IndexTaskDetailResponse from(KbIndexTask task) {
            return new IndexTaskDetailResponse(
                    task.getId(),
                    task.getDocumentId(),
                    task.getKbId(),
                    task.getStatus(),
                    task.getProgressPercent(),
                    task.getProgressPhase(),
                    task.getRetryCount(),
                    task.isRecoverable(),
                    task.getNextRetryAt(),
                    task.getHeartbeatAt(),
                    task.getLeaseUntil(),
                    task.getStartedAt(),
                    task.getFinishedAt(),
                    task.getErrorMessage()
            );
        }
    }
}
