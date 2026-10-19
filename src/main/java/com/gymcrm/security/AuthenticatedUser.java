package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.ForbiddenException;

public record AuthenticatedUser(String username, UserType userType) {

    public void requireSelf(UserType expectedType, String targetUsername) {
        if (userType != expectedType || !username.equals(targetUsername)) {
            throw new ForbiddenException("Access denied");
        }
    }
}
