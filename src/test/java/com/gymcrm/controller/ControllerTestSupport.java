package com.gymcrm.controller;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.GlobalExceptionHandler;
import com.gymcrm.security.AuthenticatedUser;
import com.gymcrm.security.JwtAuthenticationToken;
import com.gymcrm.security.VerifiedToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    static AuthenticatedUser authenticateAs(String username, UserType userType) {
        AuthenticatedUser user = new AuthenticatedUser(username, userType);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                new VerifiedToken("test-token", user, Instant.now().plusSeconds(60))));
        return user;
    }

    static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
