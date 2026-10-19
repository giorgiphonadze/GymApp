package com.gymcrm.exception;

/**
 * Thrown when credentials (username/password) are missing, malformed or do not match,
 * e.g. failed login, wrong old password on password change, or invalid Authorization header.
 */
public class PasswordException extends RuntimeException {

    public PasswordException(String message) {
        super(message);
    }

    public PasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
