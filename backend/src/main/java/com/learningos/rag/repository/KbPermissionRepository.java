package com.learningos.rag.repository;

import com.learningos.rag.domain.KbPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KbPermissionRepository extends JpaRepository<KbPermission, String> {
}
