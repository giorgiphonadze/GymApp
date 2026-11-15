package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenDenylistTest {
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final AuthenticatedUser USER = new AuthenticatedUser("John.Doe", UserType.TRAINEE);

    @Test
    void revokedTokenIsReportedUntilItExpires() {
        LoginAttemptServiceTest.MutableClock clock = new LoginAttemptServiceTest.MutableClock(NOW);
        TokenDenylist denylist = new TokenDenylist(clock);

        denylist.revoke(new VerifiedToken("a", USER, NOW.plus(Duration.ofMinutes(30))));

        assertTrue(denylist.isRevoked("a"));
        assertFalse(denylist.isRevoked("b"));
        clock.advance(Duration.ofMinutes(30));
        assertFalse(denylist.isRevoked("a"));
    }

    @Test
    void expiredEntriesArePurgedOnNextRevocation() {
        LoginAttemptServiceTest.MutableClock clock = new LoginAttemptServiceTest.MutableClock(NOW);
        TokenDenylist denylist = new TokenDenylist(clock);
        denylist.revoke(new VerifiedToken("a", USER, NOW.plus(Duration.ofMinutes(1))));

        clock.advance(Duration.ofMinutes(2));
        denylist.revoke(new VerifiedToken("b", USER, clock.instant().plus(Duration.ofMinutes(30))));

        assertEquals(1, denylist.size());
    }
}
