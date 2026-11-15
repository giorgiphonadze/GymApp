package com.gymcrm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "gym.security")
public record GymSecurityProperties(BruteForce bruteForce, Cors cors) {

    public record BruteForce(int maxAttempts, Duration blockDuration) {
    }

    public record Cors(List<String> allowedOrigins, List<String> allowedMethods, List<String> allowedHeaders,
                       List<String> exposedHeaders, Duration maxAge) {
    }
}
