package com.learningos.common.auth;

import java.util.List;

public record UserContext(String userId, String displayName, List<String> roles) {

    public UserContext {
        roles = List.copyOf(roles);
    }
}
