package com.gymcrm.security;

import java.time.Instant;

public record VerifiedToken(String id, AuthenticatedUser user, Instant expiresAt) {
}
