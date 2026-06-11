package com.learningos.user.application;

import com.learningos.common.api.ErrorCode;
import com.learningos.common.exception.ApiException;
import com.learningos.user.domain.AppUser;
import com.learningos.user.dto.UserDtos.CreateUserRequest;
import com.learningos.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository userRepository;

    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(ErrorCode.CONFLICT, "Username already exists");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser get(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public AppUser get(String currentUserId, boolean currentUserAdmin, String userId) {
        if (!currentUserAdmin && !currentUserId.equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "User detail access denied");
        }
        return get(userId);
    }
}
