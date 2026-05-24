package com.example.poetry.backend.user.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.poetry.backend.user.security.VerificationCodeStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailServiceTest {

    private VerificationCodeStore codeStore;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        codeStore = new VerificationCodeStore();
        emailService = new EmailService(codeStore);
    }

    @Test
    void sendVerificationCode_storesValidCodeForEmail() {
        emailService.sendVerificationCode("User@Test.com", 6, 5);

        assertTrue(codeStore.hasValidCode("user@test.com"));
    }
}
