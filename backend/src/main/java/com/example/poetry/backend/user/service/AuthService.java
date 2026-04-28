package com.example.poetry.backend.user.service;

import com.example.poetry.backend.user.dto.AuthResponse;
import com.example.poetry.backend.user.dto.ChangePasswordRequest;
import com.example.poetry.backend.user.dto.LoginRequest;
import com.example.poetry.backend.user.dto.RegisterRequest;
import com.example.poetry.backend.user.dto.UserProfileResponse;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAuthRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAuthRepository repository, JwtTokenProvider jwtTokenProvider) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
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
        UserAccount user = repository.findByAccount(request.account().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        String token = jwtTokenProvider.generateToken(user.userId(), user.username(), user.nickname());
        return new AuthResponse(token, user.userId(), user.username(), user.nickname());
    }

    public UserProfileResponse me(String authHeader) {
        String token = extractBearerToken(authHeader);
        Long userId = jwtTokenProvider.parseUserId(token);
        UserAccount user = repository.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或登录已失效"));
        return new UserProfileResponse(user.userId(), user.username(), user.nickname(), user.email());
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
