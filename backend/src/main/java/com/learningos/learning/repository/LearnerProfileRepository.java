package com.learningos.learning.repository;

import com.learningos.learning.domain.LearnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, String> {

    long countByLearnerId(String learnerId);

    Optional<LearnerProfile> findFirstByLearnerIdOrderByUpdatedAtDesc(String learnerId);
}
