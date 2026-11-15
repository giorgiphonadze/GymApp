package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.LoginBlockedException;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    private SimpleMeterRegistry registry;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        LoginAttemptService attempts = new LoginAttemptService(3, Duration.ofMinutes(5),
                new LoginAttemptServiceTest.MutableClock(Instant.parse("2026-01-01T10:00:00Z")));
        loginService = new LoginService(authenticationManager, attempts, new GymMetrics(registry));
    }

    private void acceptCredentials(String username, String role) {
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList(role)));
    }

    @Test
    void successfulLoginReturnsUserWithTypeFromRole() {
        acceptCredentials("coach", "ROLE_TRAINER");

        AuthenticatedUser user = loginService.login("coach", "pass");

        assertEquals(new AuthenticatedUser("coach", UserType.TRAINER), user);
        assertEquals(1.0, registry.get("gym.authentications")
                .tag("userType", "trainer").tag("result", "success").counter().count());
    }

    @Test
    void badCredentialsFailWithGenericMessage() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        PasswordException e = assertThrows(PasswordException.class, () -> loginService.login("user", "wrong"));

        assertEquals("Login failed: invalid username or password", e.getMessage());
        assertEquals(1.0, registry.get("gym.authentications").tag("result", "failure").counter().count());
    }

    @Test
    void threeFailuresBlockFurtherAttemptsWithoutCheckingPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        for (int i = 0; i < 3; i++) {
            assertThrows(PasswordException.class, () -> loginService.login("user", "wrong"));
        }

        LoginBlockedException e = assertThrows(LoginBlockedException.class,
                () -> loginService.login("user", "correct"));

        assertEquals("Too many failed login attempts. Try again in 5 minute(s)", e.getMessage());
        assertEquals(Duration.ofMinutes(5), e.getRetryAfter());
    }

    @Test
    void blockedUserIsNotPassedToAuthenticationManager() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        for (int i = 0; i < 3; i++) {
            assertThrows(PasswordException.class, () -> loginService.login("user", "wrong"));
        }

        assertThrows(LoginBlockedException.class, () -> loginService.login("user", "correct"));

        verify(authenticationManager, times(3)).authenticate(any());
    }

    @Test
    void otherUsersAreNotAffectedByBlock() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        for (int i = 0; i < 3; i++) {
            assertThrows(PasswordException.class, () -> loginService.login("user", "wrong"));
        }

        assertThrows(PasswordException.class, () -> loginService.login("other", "wrong"));
    }
}
