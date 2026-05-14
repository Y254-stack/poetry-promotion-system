package com.example.poetry.backend.user.security;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 针对「账号不存在」等无法落库的失败尝试，按登录账号串做短期限流，减轻撞库与暴力枚举。
 */
@Component
public class LoginBruteForceGuard {

    private static final int MAX_ATTEMPTS = 12;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public String normalizeKey(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }

    public void checkAllowed(String accountKey) {
        if (accountKey.isEmpty()) {
            return;
        }
        Window w = windows.get(accountKey);
        if (w == null) {
            return;
        }
        synchronized (w) {
            if (now() - w.startedAtMs > WINDOW_MS) {
                windows.remove(accountKey, w);
                return;
            }
            if (w.failures >= MAX_ATTEMPTS) {
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "登录尝试过于频繁，请稍后再试"
                );
            }
        }
    }

    public void recordFailure(String accountKey) {
        if (accountKey.isEmpty()) {
            return;
        }
        windows.compute(accountKey, (k, existing) -> {
            long now = now();
            if (existing == null) {
                return new Window(now, 1);
            }
            synchronized (existing) {
                if (now - existing.startedAtMs > WINDOW_MS) {
                    existing.startedAtMs = now;
                    existing.failures = 1;
                } else {
                    existing.failures++;
                }
                return existing;
            }
        });
    }

    public void clearFailures(String accountKey) {
        if (!accountKey.isEmpty()) {
            windows.remove(accountKey);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static final class Window {
        volatile long startedAtMs;
        volatile int failures;

        Window(long startedAtMs, int failures) {
            this.startedAtMs = startedAtMs;
            this.failures = failures;
        }
    }
}
