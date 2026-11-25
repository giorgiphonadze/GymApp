package com.gymcrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tokens revoked by logout. Entries are only kept until the token would have expired anyway,
 * because an expired token is rejected by signature/expiry validation regardless.
 */
@Component
public class TokenDenylist {
    private static final Logger log = LoggerFactory.getLogger(TokenDenylist.class);

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public TokenDenylist() {
        this(Clock.systemUTC());
    }

    TokenDenylist(Clock clock) {
        this.clock = clock;
    }

    public void revoke(VerifiedToken token) {
        purgeExpired();
        revoked.put(token.id(), token.expiresAt());
        log.info("Revoked token of {} account", token.user().userType().name().toLowerCase());
    }

    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revoked.get(tokenId);
        return expiresAt != null && expiresAt.isAfter(clock.instant());
    }

    int size() {
        return revoked.size();
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        revoked.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
    }
}
