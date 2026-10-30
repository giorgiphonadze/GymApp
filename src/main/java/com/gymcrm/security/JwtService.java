package com.gymcrm.security;

import com.gymcrm.domain.UserType;
import com.gymcrm.exception.PasswordException;
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
        if (authHeader == null || authHeader.isBlank()) {
            throw new PasswordException("Authorization header is missing");
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            throw new PasswordException("Authorization header must use the Bearer scheme");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new PasswordException("Bearer token is empty");
        }
        Claims claims = parseClaims(token);
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new PasswordException("Token does not identify a user");
        }
        return new AuthenticatedUser(claims.getSubject(), parseUserType(claims.get(USER_TYPE_CLAIM, String.class)));
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
            throw new PasswordException("Token has expired", e);
        } catch (SignatureException e) {
            throw new PasswordException("Token signature is invalid", e);
        } catch (UnsupportedJwtException e) {
            throw new PasswordException("Token must be signed", e);
        } catch (MalformedJwtException e) {
            throw new PasswordException("Token is malformed", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new PasswordException("Token could not be verified", e);
        }
    }

    private static UserType parseUserType(String userType) {
        if (userType == null) {
            throw new PasswordException("Token does not specify a user type");
        }
        try {
            return UserType.valueOf(userType);
        } catch (IllegalArgumentException e) {
            throw new PasswordException("Token contains an unknown user type", e);
        }
    }
}
