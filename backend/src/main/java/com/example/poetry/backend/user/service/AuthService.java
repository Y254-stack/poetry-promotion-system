package com.example.poetry.backend.user.service;

import com.example.poetry.backend.user.dto.AuthResponse;
import com.example.poetry.backend.user.dto.ChangePasswordRequest;
import com.example.poetry.backend.user.dto.LoginRequest;
import com.example.poetry.backend.user.dto.RegisterRequest;
import com.example.poetry.backend.user.dto.UserProfileResponse;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import com.example.poetry.backend.user.security.LoginBruteForceGuard;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;
    private static final int LOGIN_LOCK_MINUTES = 15;

    private final UserAuthRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginBruteForceGuard loginBruteForceGuard;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
        UserAuthRepository repository,
        JwtTokenProvider jwtTokenProvider,
        LoginBruteForceGuard loginBruteForceGuard
    ) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginBruteForceGuard = loginBruteForceGuard;
    }

    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        String hash = passwordEncoder.encode(request.password());
        long userId = repository.createUser(
            request.username().trim(),
            hash,
            request.nickname().trim(),
            request.email().trim()
        );
        String token = jwtTokenProvider.generateToken(userId, request.username(), request.nickname());
        return new AuthResponse(token, userId, request.username(), request.nickname());
    }

    public AuthResponse login(LoginRequest request) {
        String accountRaw = request.account().trim();
        String guardKey = loginBruteForceGuard.normalizeKey(accountRaw);
        loginBruteForceGuard.checkAllowed(guardKey);

        var userOpt = repository.findByAccount(accountRaw);
        if (userOpt.isEmpty()) {
            loginBruteForceGuard.recordFailure(guardKey);
            loginBruteForceGuard.checkAllowed(guardKey);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        UserAccount user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();
        if (user.loginLockedUntil() != null && user.loginLockedUntil().isAfter(now)) {
            throw new ResponseStatusException(
                HttpStatus.LOCKED,
                "因连续登录失败，账号已暂时锁定，请稍后再试"
            );
        }

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            onWrongPassword(user);
        }

        repository.updateLoginSecurity(user.userId(), null, 0, null);
        loginBruteForceGuard.clearFailures(guardKey);
        String token = jwtTokenProvider.generateToken(user.userId(), user.username(), user.nickname());
        return new AuthResponse(token, user.userId(), user.username(), user.nickname());
    }

    private void onWrongPassword(UserAccount user) {
        LocalDateTime now = LocalDateTime.now();
        int count = user.failedLoginCount();
        LocalDateTime windowStart = user.failedLoginWindowStart();

        boolean windowExpired = windowStart == null
            || Duration.between(windowStart, now).toMinutes() > LOGIN_WINDOW_MINUTES;

        if (windowExpired) {
            repository.updateLoginSecurity(user.userId(), null, 1, now);
            int remaining = LOGIN_MAX_ATTEMPTS - 1;
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "密码错误，还可尝试 " + remaining + " 次"
            );
        }

        int nextCount = count + 1;
        if (nextCount >= LOGIN_MAX_ATTEMPTS) {
            LocalDateTime lockedUntil = now.plusMinutes(LOGIN_LOCK_MINUTES);
            repository.updateLoginSecurity(user.userId(), lockedUntil, 0, null);
            throw new ResponseStatusException(
                HttpStatus.LOCKED,
                "因连续登录失败，账号已暂时锁定，请稍后再试"
            );
        }

        repository.updateLoginSecurity(user.userId(), null, nextCount, windowStart);
        int remaining = LOGIN_MAX_ATTEMPTS - nextCount;
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "密码错误，还可尝试 " + remaining + " 次"
        );
    }

    public UserProfileResponse me(String authHeader) {
        String token = extractBearerToken(authHeader);
        Long userId = jwtTokenProvider.parseUserId(token);
        UserAccount user = repository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或登录已失效"));
        return new UserProfileResponse(user.userId(), user.username(), user.nickname(), user.email());
    }

    /**
     * 从 Authorization 解析当前用户 ID，并校验账号仍存在。
     */
    public long requireUserId(String authHeader) {
        String token = extractBearerToken(authHeader);
        long userId = jwtTokenProvider.parseUserId(token);
        repository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或登录已失效"));
        return userId;
    }

    public AuthResponse changePassword(String authHeader, ChangePasswordRequest request) {
        String token = extractBearerToken(authHeader);
        Long userId = jwtTokenProvider.parseUserId(token);
        UserAccount user = repository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或登录已失效"));

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前密码错误");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        repository.updatePassword(userId, newHash);

        // 密码修改后生成新token
        String newToken = jwtTokenProvider.generateToken(user.userId(), user.username(), user.nickname());
        return new AuthResponse(newToken, user.userId(), user.username(), user.nickname());
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 Bearer Token");
        }
        return authHeader.substring("Bearer ".length()).trim();
    }
}
