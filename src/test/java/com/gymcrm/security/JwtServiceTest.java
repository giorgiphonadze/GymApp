package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.ForbiddenException;
import com.gymcrm.exception.PasswordException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-0123456789-abcdefghij";
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static JwtService serviceAt(Instant instant) {
        return new JwtService(SECRET, Duration.ofMinutes(30), clockAt(instant));
    }

    private static void assertRejected(String expectedMessage, Executable executable) {
        assertEquals(expectedMessage, assertThrows(PasswordException.class, executable).getMessage());
    }

    private static String signedToken(String subject, String userType) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder().subject(subject).expiration(Date.from(NOW.plus(Duration.ofHours(1))));
        if (userType != null) {
            builder.claim("userType", userType);
        }
        return builder.signWith(key).compact();
    }

    @Test
    void tokenRoundTripsUsernameAndUserType() {
        JwtService service = serviceAt(NOW);

        AuthenticatedUser user = service.authenticate("Bearer " + service.generateToken("John.Doe", UserType.TRAINEE));

        assertEquals("John.Doe", user.username());
        assertEquals(UserType.TRAINEE, user.userType());
    }

    @Test
    void exposesLifetimeInSeconds() {
        assertEquals(1800, serviceAt(NOW).getExpiresInSeconds());
    }

    @Test
    void tokenIsValidJustBeforeExpiryAndRejectedAfterwards() {
        String token = "Bearer " + serviceAt(NOW).generateToken("John.Doe", UserType.TRAINEE);

        assertDoesNotThrow(() -> serviceAt(NOW.plus(Duration.ofMinutes(29))).authenticate(token));
        assertRejected("Token has expired", () -> serviceAt(NOW.plus(Duration.ofMinutes(31))).authenticate(token));
    }

    @Test
    void missingOrNonBearerHeaderIsRejected() {
        JwtService service = serviceAt(NOW);

        assertRejected("Authorization header is missing", () -> service.authenticate(null));
        assertRejected("Authorization header is missing", () -> service.authenticate(" "));
        assertRejected("Authorization header must use the Bearer scheme",
                () -> service.authenticate("Basic dXNlcjpwYXNz"));
        assertRejected("Bearer token is empty", () -> service.authenticate("Bearer "));
    }

    @Test
    void garbageTokenIsRejected() {
        assertRejected("Token is malformed", () -> serviceAt(NOW).authenticate("Bearer not.a.jwt"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-0123456789", Duration.ofMinutes(30),
                clockAt(NOW));
        String forged = other.generateToken("John.Doe", UserType.TRAINEE);

        assertRejected("Token signature is invalid", () -> serviceAt(NOW).authenticate("Bearer " + forged));
    }

    @Test
    void unsignedTokenIsRejected() {
        String unsigned = Jwts.builder().subject("John.Doe").claim("userType", "TRAINEE")
                .expiration(Date.from(NOW.plus(Duration.ofHours(1)))).compact();

        assertRejected("Token must be signed", () -> serviceAt(NOW).authenticate("Bearer " + unsigned));
    }

    @Test
    void tokenWithUnknownUserTypeIsRejected() {
        String token = signedToken("John.Doe", "ADMIN");

        assertRejected("Token contains an unknown user type", () -> serviceAt(NOW).authenticate("Bearer " + token));
    }

    @Test
    void tokenWithoutUserTypeIsRejected() {
        String token = signedToken("John.Doe", null);

        assertRejected("Token does not specify a user type", () -> serviceAt(NOW).authenticate("Bearer " + token));
    }

    @Test
    void tokenWithoutSubjectIsRejected() {
        String token = signedToken(null, "TRAINEE");

        assertRejected("Token does not identify a user", () -> serviceAt(NOW).authenticate("Bearer " + token));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 30));
    }

    @Test
    void nonPositiveExpirationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 0));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, -5));
    }

    @Test
    void requireSelfAcceptsOwnerOfMatchingType() {
        AuthenticatedUser user = new AuthenticatedUser("John.Doe", UserType.TRAINEE);

        assertDoesNotThrow(() -> user.requireSelf(UserType.TRAINEE, "John.Doe"));
    }

    @Test
    void requireSelfRejectsOtherUsersAndOtherUserTypes() {
        AuthenticatedUser user = new AuthenticatedUser("John.Doe", UserType.TRAINEE);

        assertEquals("Access denied: the token does not belong to the requested trainee account",
                assertThrows(ForbiddenException.class,
                        () -> user.requireSelf(UserType.TRAINEE, "Someone.Else")).getMessage());
        assertEquals("Access denied: this resource requires a trainer token, but a trainee token was provided",
                assertThrows(ForbiddenException.class,
                        () -> user.requireSelf(UserType.TRAINER, "John.Doe")).getMessage());
    }
}
