package com.gymcrm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {
    private static final Duration BLOCK = Duration.ofMinutes(5);

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        service = new LoginAttemptService(3, BLOCK, clock);
    }

    @Test
    void userIsNotBlockedBeforeThirdFailure() {
        service.loginFailed("john");
        service.loginFailed("john");

        assertTrue(service.remainingBlockTime("john").isEmpty());
    }

    @Test
    void thirdConsecutiveFailureBlocksForFiveMinutes() {
        service.loginFailed("john");
        service.loginFailed("john");
        service.loginFailed("john");

        assertEquals(Optional.of(BLOCK), service.remainingBlockTime("john"));
        clock.advance(Duration.ofMinutes(4));
        assertEquals(Optional.of(Duration.ofMinutes(1)), service.remainingBlockTime("john"));
    }

    @Test
    void blockExpiresAfterFiveMinutesAndCounterStartsOver() {
        for (int i = 0; i < 3; i++) {
            service.loginFailed("john");
        }

        clock.advance(BLOCK);
        assertTrue(service.remainingBlockTime("john").isEmpty());

        service.loginFailed("john");
        assertTrue(service.remainingBlockTime("john").isEmpty());
    }

    @Test
    void successfulLoginResetsFailures() {
        service.loginFailed("john");
        service.loginFailed("john");
        service.loginSucceeded("john");
        service.loginFailed("john");

        assertTrue(service.remainingBlockTime("john").isEmpty());
    }

    @Test
    void failuresAreTrackedPerUsername() {
        for (int i = 0; i < 3; i++) {
            service.loginFailed("john");
        }

        assertTrue(service.remainingBlockTime("john").isPresent());
        assertTrue(service.remainingBlockTime("jane").isEmpty());
    }

    @Test
    void oldFailuresDoNotCountTowardsBlock() {
        service.loginFailed("john");
        service.loginFailed("john");
        clock.advance(BLOCK);
        service.loginFailed("john");

        assertTrue(service.remainingBlockTime("john").isEmpty());
    }

    @Test
    void invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptService(0, BLOCK, clock));
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptService(3, Duration.ZERO, clock));
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
