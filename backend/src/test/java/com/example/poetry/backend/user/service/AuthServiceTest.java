package com.example.poetry.backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.poetry.backend.user.dto.AuthResponse;
import com.example.poetry.backend.user.dto.ChangePasswordRequest;
import com.example.poetry.backend.user.dto.LoginRequest;
import com.example.poetry.backend.user.dto.RegisterRequest;
import com.example.poetry.backend.user.dto.ResetPasswordRequest;
import com.example.poetry.backend.user.dto.SendVerificationCodeRequest;
import com.example.poetry.backend.user.repository.UserAccount;
import com.example.poetry.backend.user.repository.UserAuthRepository;
import com.example.poetry.backend.user.security.JwtTokenProvider;
import com.example.poetry.backend.user.security.LoginBruteForceGuard;
import com.example.poetry.backend.user.security.VerificationCodeStore;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PLAIN_PASSWORD = "secret12";

    @Mock
    private UserAuthRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private LoginBruteForceGuard loginBruteForceGuard;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationCodeStore codeStore;

    private AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            repository,
            jwtTokenProvider,
            loginBruteForceGuard,
            emailService,
            codeStore
        );
        lenient().when(loginBruteForceGuard.normalizeKey(anyString()))
            .thenAnswer(invocation -> {
                String account = invocation.getArgument(0, String.class);
                return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
            });
    }

    // --- register ---

    @Test
    void register_success_returnsToken() {
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "昵称",
            "user@example.com",
            PLAIN_PASSWORD,
            true
        );
        when(repository.existsByUsername("newuser")).thenReturn(false);
        when(repository.existsByEmail("user@example.com")).thenReturn(false);
        when(repository.createUser(eq("newuser"), anyString(), eq("昵称"), eq("user@example.com")))
            .thenReturn(42L);
        when(jwtTokenProvider.generateToken(42L, "newuser", "昵称")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals(42L, response.userId());
        assertEquals("newuser", response.username());
        verify(repository).createUser(eq("newuser"), anyString(), eq("昵称"), eq("user@example.com"));
    }

    @Test
    void register_usernameExists_throwsConflict() {
        RegisterRequest request = new RegisterRequest(
            "taken",
            "昵称",
            "user@example.com",
            PLAIN_PASSWORD,
            true
        );
        when(repository.existsByUsername("taken")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("用户名已存在", ex.getReason());
        verify(repository, never()).createUser(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void register_emailExists_throwsConflict() {
        RegisterRequest request = new RegisterRequest(
            "newuser",
            "昵称",
            "dup@example.com",
            PLAIN_PASSWORD,
            true
        );
        when(repository.existsByUsername("newuser")).thenReturn(false);
        when(repository.existsByEmail("dup@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("邮箱已被注册", ex.getReason());
    }

    // --- login ---

    @Test
    void login_success_clearsFailuresAndReturnsToken() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(repository.findByAccount("tester")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(1L, "tester", "昵称")).thenReturn("login-jwt");

        AuthResponse response = authService.login(new LoginRequest("tester", PLAIN_PASSWORD));

        assertEquals("login-jwt", response.token());
        verify(loginBruteForceGuard).checkAllowed("tester");
        verify(loginBruteForceGuard).clearFailures("tester");
        verify(repository).updateLoginSecurity(1L, null, 0, null);
    }

    @Test
    void login_accountNotFound_recordsFailureAndThrowsUnauthorized() {
        when(repository.findByAccount("ghost")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("ghost", PLAIN_PASSWORD))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("账号或密码错误", ex.getReason());
        verify(loginBruteForceGuard).recordFailure("ghost");
        verify(loginBruteForceGuard, times(2)).checkAllowed("ghost");
    }

    @Test
    void login_guardBlocks_throwsTooManyRequests() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请稍后再试"))
            .when(loginBruteForceGuard).checkAllowed("blocked");

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("blocked", PLAIN_PASSWORD))
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(repository, never()).findByAccount(anyString());
    }

    @Test
    void login_accountLocked_throwsLocked() {
        UserAccount locked = new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(PLAIN_PASSWORD),
            "昵称",
            "tester@example.com",
            null,
            LocalDateTime.now().plusMinutes(10),
            0,
            null
        );
        when(repository.findByAccount("tester")).thenReturn(Optional.of(locked));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("tester", PLAIN_PASSWORD))
        );

        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
        assertEquals("因连续登录失败，账号已暂时锁定，请稍后再试", ex.getReason());
    }

    @Test
    void login_wrongPassword_firstFailureInWindow_showsRemainingAttempts() {
        UserAccount user = new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(PLAIN_PASSWORD),
            "昵称",
            "tester@example.com",
            null,
            null,
            0,
            null
        );
        when(repository.findByAccount("tester")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("tester", "wrong-pass"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("密码错误，还可尝试 4 次", ex.getReason());
        verify(repository).updateLoginSecurity(eq(1L), isNull(), eq(1), any(LocalDateTime.class));
    }

    @Test
    void login_wrongPassword_incrementsCountWithinWindow() {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(2);
        UserAccount user = new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(PLAIN_PASSWORD),
            "昵称",
            "tester@example.com",
            null,
            null,
            2,
            windowStart
        );
        when(repository.findByAccount("tester")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("tester", "wrong-pass"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("密码错误，还可尝试 2 次", ex.getReason());
        verify(repository).updateLoginSecurity(eq(1L), isNull(), eq(3), eq(windowStart));
    }

    @Test
    void login_wrongPassword_maxAttempts_locksAccount() {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(2);
        UserAccount user = new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(PLAIN_PASSWORD),
            "昵称",
            "tester@example.com",
            null,
            null,
            4,
            windowStart
        );
        when(repository.findByAccount("tester")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("tester", "wrong-pass"))
        );

        assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
        assertEquals("因连续登录失败，账号已暂时锁定，请稍后再试", ex.getReason());
        verify(repository).updateLoginSecurity(eq(1L), any(LocalDateTime.class), eq(0), isNull());
    }

    // --- change password ---

    @Test
    void changePassword_wrongCurrentPassword_throwsUnauthorized() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.changePassword(
                "Bearer valid-token",
                new ChangePasswordRequest("wrong-old", "newpass12")
            )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("当前密码错误", ex.getReason());
        verify(repository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void changePassword_success_returnsNewToken() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(1L, "tester", "昵称")).thenReturn("new-jwt");

        AuthResponse response = authService.changePassword(
            "Bearer valid-token",
            new ChangePasswordRequest(PLAIN_PASSWORD, "newpass99")
        );

        assertEquals("new-jwt", response.token());
        verify(repository).updatePassword(eq(1L), anyString());
    }

    // --- verification / reset ---

    @Test
    void sendVerificationCode_unknownEmail_throwsNotFound() {
        when(repository.existsByEmail("nobody@example.com")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.sendVerificationCode(new SendVerificationCodeRequest("nobody@example.com"))
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("该邮箱未注册", ex.getReason());
        verify(emailService, never()).sendVerificationCode(anyString(), anyInt(), anyInt());
    }

    @Test
    void sendVerificationCode_registeredEmail_sendsCode() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);

        authService.sendVerificationCode(new SendVerificationCodeRequest("  User@Example.COM  "));

        verify(emailService).sendVerificationCode("user@example.com", 6, 5);
    }

    @Test
    void resetPassword_invalidCode_throwsBadRequest() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        when(codeStore.verifyAndConsume("user@example.com", "000000")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.resetPassword(
                new ResetPasswordRequest("user@example.com", "000000", "newpass12")
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("验证码错误或已过期", ex.getReason());
    }

    // --- me / requireUserId / updateNickname ---

    @Test
    void me_success_returnsProfile() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));

        var profile = authService.me("Bearer valid-token");

        assertEquals(1L, profile.userId());
        assertEquals("tester", profile.username());
        assertEquals("昵称", profile.nickname());
        assertEquals("tester@example.com", profile.email());
    }

    @Test
    void me_missingBearer_throwsUnauthorized() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.me(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("缺少 Bearer Token", ex.getReason());
    }

    @Test
    void me_userNotFound_throwsUnauthorized() {
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(99L);
        when(repository.findByUserId(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.me("Bearer valid-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("用户不存在或登录已失效", ex.getReason());
    }

    @Test
    void requireUserId_success_returnsUserId() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));

        assertEquals(1L, authService.requireUserId("Bearer valid-token"));
    }

    @Test
    void updateNickname_blank_throwsBadRequest() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.updateNickname("Bearer valid-token", "   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("昵称不能为空", ex.getReason());
        verify(repository, never()).updateNickname(anyLong(), anyString());
    }

    @Test
    void updateNickname_success_trimsAndUpdates() {
        UserAccount user = userWithPassword(PLAIN_PASSWORD);
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(1L);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(user));

        authService.updateNickname("Bearer valid-token", "  新昵称  ");

        verify(repository).updateNickname(1L, "新昵称");
    }

    @Test
    void changePassword_missingBearer_throwsUnauthorized() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.changePassword(
                "TokenWithoutBearer",
                new ChangePasswordRequest(PLAIN_PASSWORD, "newpass12")
            )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("缺少 Bearer Token", ex.getReason());
    }

    @Test
    void changePassword_userNotFound_throwsUnauthorized() {
        when(jwtTokenProvider.parseUserId("valid-token")).thenReturn(88L);
        when(repository.findByUserId(88L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.changePassword(
                "Bearer valid-token",
                new ChangePasswordRequest(PLAIN_PASSWORD, "newpass12")
            )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("用户不存在或登录已失效", ex.getReason());
    }

    @Test
    void login_wrongPassword_windowExpired_resetsFailureCount() {
        UserAccount user = new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(PLAIN_PASSWORD),
            "昵称",
            "tester@example.com",
            null,
            null,
            3,
            LocalDateTime.now().minusMinutes(20)
        );
        when(repository.findByAccount("tester")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("tester", "wrong-pass"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("密码错误，还可尝试 4 次", ex.getReason());
        verify(repository).updateLoginSecurity(eq(1L), isNull(), eq(1), any(LocalDateTime.class));
    }

    @Test
    void resetPassword_userMissingAfterValidCode_throwsNotFound() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        when(codeStore.verifyAndConsume("user@example.com", "123456")).thenReturn(true);
        when(repository.findByAccount("user@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.resetPassword(
                new ResetPasswordRequest("user@example.com", "123456", "newpass12")
            )
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("用户不存在", ex.getReason());
        verify(repository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void resetPassword_success_clearsLockAndReturnsToken() {
        UserAccount user = userWithEmail("user@example.com", PLAIN_PASSWORD);
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        when(codeStore.verifyAndConsume("user@example.com", "123456")).thenReturn(true);
        when(repository.findByAccount("user@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(1L, "tester", "昵称")).thenReturn("reset-jwt");

        AuthResponse response = authService.resetPassword(
            new ResetPasswordRequest("user@example.com", "123456", "brand-new12")
        );

        assertNotNull(response);
        assertEquals("reset-jwt", response.token());
        verify(repository).updatePassword(eq(1L), anyString());
        verify(repository).updateLoginSecurity(1L, null, 0, null);
    }

    private UserAccount userWithPassword(String plainPassword) {
        return userWithEmail("tester@example.com", plainPassword);
    }

    private UserAccount userWithEmail(String email, String plainPassword) {
        return new UserAccount(
            1L,
            "tester",
            passwordEncoder.encode(plainPassword),
            "昵称",
            email,
            null,
            null,
            0,
            null
        );
    }
}
