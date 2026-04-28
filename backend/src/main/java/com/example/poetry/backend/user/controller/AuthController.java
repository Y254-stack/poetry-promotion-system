package com.example.poetry.backend.user.controller;

import com.example.poetry.backend.user.dto.AuthResponse;
import com.example.poetry.backend.user.dto.ChangePasswordRequest;
import com.example.poetry.backend.user.dto.LoginRequest;
import com.example.poetry.backend.user.dto.RegisterRequest;
import com.example.poetry.backend.user.dto.UserProfileResponse;
import com.example.poetry.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return authService.me(authorization);
    }

    @PostMapping("/change-password")
    public AuthResponse changePassword(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        return authService.changePassword(authorization, request);
    }
}
