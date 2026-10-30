package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.ForbiddenException;

public record AuthenticatedUser(String username, UserType userType) {

    public void requireSelf(UserType expectedType, String targetUsername) {
        if (userType != expectedType) {
            throw new ForbiddenException("Access denied: this resource requires a " + label(expectedType)
                    + " token, but a " + label(userType) + " token was provided");
        }
        if (!username.equals(targetUsername)) {
            throw new ForbiddenException("Access denied: the token does not belong to the requested "
                    + label(expectedType) + " account");
        }
    }

    private static String label(UserType type) {
        return type.name().toLowerCase();
    }
}
