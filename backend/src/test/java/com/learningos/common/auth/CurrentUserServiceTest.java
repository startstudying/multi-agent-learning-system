package com.learningos.common.auth;

import com.learningos.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService(new AppProperties("test", "X-Trace-Id"));

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void returnsCurrentUserIdFromContext() {
        UserContextHolder.setCurrentUser(new UserContext("alice", "Alice", List.of("USER")));

        assertThat(currentUserService.currentUserId()).isEqualTo("alice");
    }

    @Test
    void returnsCurrentUserFromContext() {
        UserContextHolder.setCurrentUser(new UserContext("alice", "Alice", List.of("STUDENT")));

        UserContext currentUser = currentUserService.currentUser();

        assertThat(currentUser.userId()).isEqualTo("alice");
        assertThat(currentUser.roles()).containsExactly("STUDENT");
    }

    @Test
    void fallsBackToDevUserWhenNoContextIsAvailable() {
        assertThat(currentUserService.currentUserId()).isEqualTo("dev_user");
        assertThat(currentUserService.currentUser().roles()).containsExactly("USER");
    }

    @Test
    void productionDoesNotFallbackToDevUserWhenNoContextIsAvailable() {
        CurrentUserService prodService = new CurrentUserService(new AppProperties("production", "X-Trace-Id"));

        assertThat(prodService.currentUserId()).isEqualTo("unauthenticated");
        assertThat(prodService.currentUser().roles()).containsExactly("USER");
        assertThat(prodService.isAdmin()).isFalse();
        assertThat(prodService.isTeacherUser()).isFalse();
    }

    @Test
    void adminAndTeacherChecksPreferRoles() {
        UserContextHolder.setCurrentUser(new UserContext("student_1", "Student One", List.of("ADMIN", "TEACHER")));

        assertThat(currentUserService.isAdmin()).isTrue();
        assertThat(currentUserService.isTeacherUser()).isTrue();
    }

    @Test
    void devCompatibilityStillAllowsLegacyAdminAndTeacherIds() {
        CurrentUserService devService = new CurrentUserService(new AppProperties("dev", "X-Trace-Id"));

        UserContextHolder.setCurrentUser(new UserContext("admin", "admin", List.of("USER")));
        assertThat(devService.isAdmin()).isTrue();

        UserContextHolder.setCurrentUser(new UserContext("teacher_1", "teacher_1", List.of("USER")));
        assertThat(devService.isTeacherUser()).isTrue();
    }

    @Test
    void productionDoesNotInferAdminOrTeacherFromUserIdStrings() {
        CurrentUserService prodService = new CurrentUserService(new AppProperties("production", "X-Trace-Id"));

        UserContextHolder.setCurrentUser(new UserContext("admin", "admin", List.of("USER")));
        assertThat(prodService.isAdmin()).isFalse();

        UserContextHolder.setCurrentUser(new UserContext("teacher_1", "teacher_1", List.of("USER")));
        assertThat(prodService.isTeacherUser()).isFalse();
    }
}
