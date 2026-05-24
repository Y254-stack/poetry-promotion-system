package com.example.poetry.backend.user.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LoginBruteForceGuardTest {

    private LoginBruteForceGuard guard;

    @BeforeEach
    void setUp() {
        guard = new LoginBruteForceGuard();
    }

    @Test
    void normalizeKey_trimsAndLowercases() {
        assertEquals("user@example.com", guard.normalizeKey("  User@Example.COM  "));
    }

    @Test
    void normalizeKey_nullReturnsEmpty() {
        assertEquals("", guard.normalizeKey(null));
    }

    @Test
    void checkAllowed_emptyKey_noOp() {
        assertDoesNotThrow(() -> guard.checkAllowed(""));
    }

    @Test
    void checkAllowed_noFailures_passes() {
        assertDoesNotThrow(() -> guard.checkAllowed("alice"));
    }

    @Test
    void recordFailure_underLimit_stillAllowed() {
        String key = "bob";
        for (int i = 0; i < 11; i++) {
            guard.recordFailure(key);
        }
        assertDoesNotThrow(() -> guard.checkAllowed(key));
    }

    @Test
    void recordFailure_atLimit_blocksFurtherAttempts() {
        String key = "carol";
        for (int i = 0; i < 12; i++) {
            guard.recordFailure(key);
        }

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> guard.checkAllowed(key)
        );
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertEquals("登录尝试过于频繁，请稍后再试", ex.getReason());
    }

    @Test
    void clearFailures_resetsWindow() {
        String key = "dave";
        for (int i = 0; i < 12; i++) {
            guard.recordFailure(key);
        }
        guard.clearFailures(key);
        assertDoesNotThrow(() -> guard.checkAllowed(key));
    }

    @Test
    void expiredWindow_allowsAgain() throws Exception {
        String key = "eve";
        for (int i = 0; i < 12; i++) {
            guard.recordFailure(key);
        }

        expireWindow(key);

        assertDoesNotThrow(() -> guard.checkAllowed(key));
    }

    private void expireWindow(String key) throws Exception {
        Field windowsField = LoginBruteForceGuard.class.getDeclaredField("windows");
        windowsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> windows =
            (ConcurrentHashMap<String, Object>) windowsField.get(guard);

        Object window = windows.get(key);
        Field startedAtField = window.getClass().getDeclaredField("startedAtMs");
        startedAtField.setAccessible(true);
        startedAtField.setLong(window, System.currentTimeMillis() - (16 * 60 * 1000L));
    }
}
