package com.learningos.user.api;

import com.learningos.common.api.ApiResponse;
import com.learningos.common.auth.CurrentUserService;
import com.learningos.common.auth.UserContext;
import com.learningos.user.application.UserService;
import com.learningos.user.dto.UserDtos.CreateUserRequest;
import com.learningos.user.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(UserResponse.from(userService.create(request)));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> get(@PathVariable String userId) {
        UserContext currentUser = currentUserService.currentUser();
        return ApiResponse.success(UserResponse.from(userService.get(currentUser.userId(), isAdmin(currentUser), userId)));
    }

    private boolean isAdmin(UserContext currentUser) {
        return currentUser.roles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
    }
}
