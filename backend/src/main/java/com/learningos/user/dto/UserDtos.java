package com.learningos.user.dto;

import com.learningos.user.domain.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 120) String username,
            @Size(max = 120) String displayName,
            @Email @Size(max = 255) String email
    ) {
    }

    public record UserResponse(
            String id,
            String username,
            String displayName,
            String email,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static UserResponse from(AppUser user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
