package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    public static final String TOKEN_TYPE = "Bearer";
    private static final String USER_TYPE_CLAIM = "userType";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${gym.security.jwt.secret}") String secret,
                      @Value("${gym.security.jwt.expiration-minutes}") long expirationMinutes) {
        this(secret, Duration.ofMinutes(expirationMinutes), Clock.systemUTC());
    }

    JwtService(String secret, Duration expiration, Clock clock) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("gym.security.jwt.secret must be at least 32 characters long");
        }
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("gym.security.jwt.expiration-minutes must be positive");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = expiration;
        this.clock = clock;
    }

    public String generateToken(String username, UserType userType) {
        Instant now = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim(USER_TYPE_CLAIM, userType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public long getExpiresInSeconds() {
        return expiration.toSeconds();
    }

    public VerifiedToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Bearer token is empty");
        }
        Claims claims = parseClaims(token);
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new InvalidTokenException("Token does not identify a user");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new InvalidTokenException("Token does not have an identifier");
        }
        AuthenticatedUser user = new AuthenticatedUser(claims.getSubject(),
                parseUserType(claims.get(USER_TYPE_CLAIM, String.class)));
        return new VerifiedToken(claims.getId(), user, claims.getExpiration().toInstant());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired", e);
        } catch (SignatureException e) {
            throw new InvalidTokenException("Token signature is invalid", e);
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("Token must be signed", e);
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Token is malformed", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token could not be verified", e);
        }
    }

    private static UserType parseUserType(String userType) {
        if (userType == null) {
            throw new InvalidTokenException("Token does not specify a user type");
        }
        try {
            return UserType.valueOf(userType);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Token contains an unknown user type", e);
        }
    }
}
