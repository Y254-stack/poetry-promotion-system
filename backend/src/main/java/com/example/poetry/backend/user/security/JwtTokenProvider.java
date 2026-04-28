package com.example.poetry.backend.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long expirationSeconds;

    public JwtTokenProvider(
        @Value("${security.jwt.secret}") String jwtSecret,
        @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.jwtSecret = jwtSecret;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Long userId, String username, String nickname) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("nickname", nickname)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey())
            .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Key signingKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (RuntimeException ex) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
