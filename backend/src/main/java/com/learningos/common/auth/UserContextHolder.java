package com.learningos.common.auth;

import java.util.Optional;

public final class UserContextHolder {

    private static final ThreadLocal<UserContext> USER_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static Optional<UserContext> currentUser() {
        return Optional.ofNullable(USER_HOLDER.get());
    }

    public static void setCurrentUser(UserContext userContext) {
        if (userContext == null) {
            USER_HOLDER.remove();
            return;
        }
        USER_HOLDER.set(userContext);
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
