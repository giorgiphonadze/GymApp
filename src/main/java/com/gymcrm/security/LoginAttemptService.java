package com.gymcrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute force protection: blocks a username after too many consecutive failed logins.
 * Attempts are tracked per username, including usernames that do not exist, so the
 * response does not reveal whether an account exists.
 */
@Service
public class LoginAttemptService {
    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(GymSecurityProperties properties) {
        this(properties.bruteForce().maxAttempts(), properties.bruteForce().blockDuration(), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration blockDuration, Clock clock) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("gym.security.brute-force.max-attempts must be positive");
        }
        if (blockDuration == null || blockDuration.isZero() || blockDuration.isNegative()) {
            throw new IllegalArgumentException("gym.security.brute-force.block-duration must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    /**
     * @return how long the username stays blocked, or empty if a login may be attempted
     */
    public Optional<Duration> remainingBlockTime(String username) {
        Attempts current = attempts.get(username);
        if (current == null || current.blockedUntil() == null) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(clock.instant(), current.blockedUntil());
        if (remaining.isZero() || remaining.isNegative()) {
            attempts.remove(username, current);
            return Optional.empty();
        }
        return Optional.of(remaining);
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }

    public void loginFailed(String username) {
        Instant now = clock.instant();
        purgeStale(now);
        Attempts updated = attempts.compute(username, (key, current) -> {
            int failures = current == null || current.isStale(now, blockDuration) ? 1 : current.failures() + 1;
            Instant blockedUntil = failures >= maxAttempts ? now.plus(blockDuration) : null;
            return new Attempts(failures, now, blockedUntil);
        });
        if (updated.blockedUntil() != null) {
            log.warn("Login blocked for {} after {} consecutive failed attempts", blockDuration, updated.failures());
        }
    }

    private void purgeStale(Instant now) {
        attempts.values().removeIf(entry -> entry.isStale(now, blockDuration));
    }

    private record Attempts(int failures, Instant lastFailure, Instant blockedUntil) {
        boolean isStale(Instant now, Duration blockDuration) {
            if (blockedUntil != null) {
                return !blockedUntil.isAfter(now);
            }
            return !lastFailure.plus(blockDuration).isAfter(now);
        }
    }
}
