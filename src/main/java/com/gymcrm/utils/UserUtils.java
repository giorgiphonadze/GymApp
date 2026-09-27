package com.gymcrm.utils;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Set;

public class UserUtils {
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();

    public static String generateUsername(String firstName, String lastName, Set<String> existingUsernames) {
        String base = firstName + "." + lastName;
        String username = base;
        int suffix = 1;
        while (existingUsernames.contains(username)) {
            username = base + suffix++;
        }
        return username;
    }

    public static String generatePassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
