package com.example.poetry.backend.user.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-at-least-32-chars-long!!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 3600);
    }

    @Test
    void generateAndParseUserId_roundTrip() {
        String token = provider.generateToken(99L, "alice", "Alice");

        assertEquals(99L, provider.parseUserId(token));
    }

    @Test
    void parseClaims_containsUsernameAndNickname() {
        String token = provider.generateToken(1L, "bob", "Bob");

        Claims claims = provider.parseClaims(token);

        assertEquals("1", claims.getSubject());
        assertEquals("bob", claims.get("username", String.class));
        assertEquals("Bob", claims.get("nickname", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void parseClaims_tamperedToken_throws() {
        String token = provider.generateToken(1L, "bob", "Bob");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> provider.parseClaims(tampered));
    }

    @Test
    void parseClaims_malformedToken_throws() {
        assertThrows(JwtException.class, () -> provider.parseClaims("not-a-valid-jwt"));
    }
}
