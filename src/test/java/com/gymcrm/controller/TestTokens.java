package com.gymcrm.controller;

import com.gymcrm.domain.UserType;
import com.gymcrm.security.JwtService;

final class TestTokens {
    static final JwtService JWT_SERVICE = new JwtService("controller-tests-secret-0123456789-abcdef", 30);

    private TestTokens() {
    }

    static String trainee(String username) {
        return bearer(username, UserType.TRAINEE);
    }

    static String trainer(String username) {
        return bearer(username, UserType.TRAINER);
    }

    static String bearer(String username, UserType userType) {
        return "Bearer " + JWT_SERVICE.generateToken(username, userType);
    }
}
