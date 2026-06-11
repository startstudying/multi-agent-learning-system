package com.learningos.rag.repository;

import com.learningos.rag.domain.KbDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KbDocumentRepository extends JpaRepository<KbDocument, String> {

    Optional<KbDocument> findByIdAndDeletedAtIsNull(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from KbDocument document where document.id = :id and document.deletedAt is null")
    Optional<KbDocument> findByIdAndDeletedAtIsNullForUpdate(@Param("id") String id);

    Optional<KbDocument> findByCreatedByAndRequestId(String createdBy, String requestId);

    List<KbDocument> findByKbIdAndDeletedAtIsNullOrderByCreatedAtDesc(String kbId);

    boolean existsByKbIdAndDeletedAtIsNull(String kbId);
}
