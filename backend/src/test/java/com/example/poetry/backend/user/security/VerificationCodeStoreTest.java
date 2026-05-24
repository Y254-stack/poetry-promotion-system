package com.example.poetry.backend.user.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerificationCodeStoreTest {

    private VerificationCodeStore store;

    @BeforeEach
    void setUp() {
        store = new VerificationCodeStore();
    }

    @Test
    void generate_returnsNumericCodeOfRequestedLength() {
        String code = store.generate(6);

        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void verifyAndConsume_correctCode_returnsTrueAndConsumes() {
        store.put("User@Example.com", "123456", 5);

        assertTrue(store.verifyAndConsume("user@example.com", "123456"));
        assertFalse(store.hasValidCode("user@example.com"));
    }

    @Test
    void verifyAndConsume_wrongCode_returnsFalse() {
        store.put("user@example.com", "123456", 5);

        assertFalse(store.verifyAndConsume("user@example.com", "000000"));
        assertTrue(store.hasValidCode("user@example.com"));
    }

    @Test
    void verifyAndConsume_expiredCode_returnsFalse() {
        store.put("user@example.com", "123456", -1);

        assertFalse(store.verifyAndConsume("user@example.com", "123456"));
    }

    @Test
    void hasValidCode_validEntry_returnsTrue() {
        store.put("user@example.com", "654321", 5);

        assertTrue(store.hasValidCode("user@example.com"));
    }

    @Test
    void remove_clearsStoredCode() {
        store.put("user@example.com", "123456", 5);

        store.remove("user@example.com");

        assertFalse(store.hasValidCode("user@example.com"));
    }
}
