package com.gymcrm.service;

import com.gymcrm.domain.User;

/**
 * A newly registered user together with its generated plain text password. Only the hash is stored,
 * so this is the single place the plain password is available to hand back to the caller.
 */
public record CreatedUser<T extends User>(T user, String rawPassword) {
}
