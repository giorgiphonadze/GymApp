package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.PasswordException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    static final String BEARER_PREFIX = "Bearer ";
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

    public AuthenticatedUser authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new PasswordException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(claims.getSubject(),
                    UserType.valueOf(claims.get(USER_TYPE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new PasswordException("Invalid or expired token", e);
        }
    }
}
