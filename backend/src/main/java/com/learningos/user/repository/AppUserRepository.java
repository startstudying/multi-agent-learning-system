package com.learningos.user.repository;

import com.learningos.user.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    boolean existsByUsername(String username);
}
