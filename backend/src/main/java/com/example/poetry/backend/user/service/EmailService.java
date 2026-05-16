package com.example.poetry.backend.user.service;

import com.example.poetry.backend.user.security.VerificationCodeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final VerificationCodeStore codeStore;

    public EmailService(VerificationCodeStore codeStore) {
        this.codeStore = codeStore;
    }

    public void sendVerificationCode(String email, int codeLength, int expiryMinutes) {
        String code = codeStore.generate(codeLength);
        codeStore.put(email, code, expiryMinutes);

        String text = "您的验证码是：" + code + "\n\n"
            + "有效期为 " + expiryMinutes + " 分钟。\n\n"
            + "请勿将验证码告诉他人。\n\n"
            + "感谢使用诗词推广平台！";

        log.info("=== VERIFICATION CODE for {} ===\n{}\n================================", email, text);
    }
}
