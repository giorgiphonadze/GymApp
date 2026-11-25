package com.gymcrm.exception;

import java.time.Duration;

public class LoginBlockedException extends RuntimeException {
    private final Duration retryAfter;

    public LoginBlockedException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
