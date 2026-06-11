package com.learningos.learning.repository;

import com.learningos.learning.domain.MasteryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasteryRecordRepository extends JpaRepository<MasteryRecord, String> {

    Optional<MasteryRecord> findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc(
            String learnerId,
            String knowledgePointId
    );
}
