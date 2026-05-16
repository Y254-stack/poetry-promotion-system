package com.example.poetry.backend.user.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory verification code store with expiry.
 * In production, replace with Redis or database-backed storage.
 */
@Component
public class VerificationCodeStore {

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Generate a random numeric code of the given length.
     */
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Store a code for the given email, with expiry in minutes.
     * Overwrites any existing code for the same email.
     */
    public void put(String email, String code, int expiryMinutes) {
        store.put(normalize(email), new CodeEntry(code, LocalDateTime.now().plusMinutes(expiryMinutes)));
    }

    /**
     * Check if the code matches and is not expired.
     * On success, the code is consumed (removed).
     */
    public boolean verifyAndConsume(String email, String code) {
        String key = normalize(email);
        CodeEntry entry = store.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt.isBefore(LocalDateTime.now())) {
            store.remove(key);
            return false;
        }
        if (entry.code.equals(code)) {
            store.remove(key);
            return true;
        }
        return false;
    }

    /**
     * Remove any stored code for the given email (cleanup).
     */
    public void remove(String email) {
        store.remove(normalize(email));
    }

    /**
     * Check if a code exists and is not expired (without consuming it).
     */
    public boolean hasValidCode(String email) {
        String key = normalize(email);
        CodeEntry entry = store.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt.isBefore(LocalDateTime.now())) {
            store.remove(key);
            return false;
        }
        return true;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private record CodeEntry(String code, LocalDateTime expiresAt) {}
}
