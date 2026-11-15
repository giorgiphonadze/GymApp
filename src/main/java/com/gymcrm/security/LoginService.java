package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.LoginBlockedException;
import com.gymcrm.exception.PasswordException;
import com.gymcrm.metrics.GymMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private static final String ROLE_PREFIX = "ROLE_";
    static final String LOGIN_FAILED = "Login failed: invalid username or password";

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final GymMetrics metrics;

    public LoginService(AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService,
                        GymMetrics metrics) {
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
        this.metrics = metrics;
    }

    public AuthenticatedUser login(String username, String password) {
        loginAttemptService.remainingBlockTime(username).ifPresent(remaining -> {
            log.warn("Login rejected: account is temporarily blocked after repeated failed attempts");
            throw new LoginBlockedException("Too many failed login attempts. Try again in "
                    + describe(remaining), remaining);
        });
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(username);
            metrics.recordAuthentication(null, false);
            log.warn("Login failed: {}", e.getMessage());
            throw new PasswordException(LOGIN_FAILED, e);
        }
        loginAttemptService.loginSucceeded(username);
        UserType userType = userType(authentication);
        metrics.recordAuthentication(userType, true);
        log.info("Login succeeded for {} account", userType.name().toLowerCase());
        return new AuthenticatedUser(authentication.getName(), userType);
    }

    private static UserType userType(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> UserType.valueOf(authority.substring(ROLE_PREFIX.length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no trainee or trainer role"));
    }

    private static String describe(Duration remaining) {
        long seconds = Math.max(1, remaining.toSeconds() + (remaining.toNanosPart() > 0 ? 1 : 0));
        return seconds >= 60 ? (seconds + 59) / 60 + " minute(s)" : seconds + " second(s)";
    }
}
