package com.learningos.rag.repository;

import com.learningos.rag.domain.KbIndexTask;
import com.learningos.rag.domain.enums.IndexTaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface KbIndexTaskRepository extends JpaRepository<KbIndexTask, String> {

    Optional<KbIndexTask> findFirstByDocumentIdOrderByCreatedAtDesc(String documentId);

    List<KbIndexTask> findByStatusAndUpdatedAtBefore(IndexTaskStatus status, Instant updatedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from KbIndexTask task
            where task.status = com.learningos.rag.domain.enums.IndexTaskStatus.PENDING
              and (task.nextRetryAt is null or task.nextRetryAt <= :now)
            order by task.createdAt asc
            """)
    List<KbIndexTask> findDuePendingTasksForUpdate(@Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select task
            from KbIndexTask task
            where task.status = com.learningos.rag.domain.enums.IndexTaskStatus.RUNNING
              and task.leaseUntil is not null
              and task.leaseUntil < :now
            order by task.leaseUntil asc
            """)
    List<KbIndexTask> findExpiredRunningTasksForUpdate(@Param("now") Instant now);
}
