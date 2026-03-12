package com.digitaldad.user.util;

import com.digitaldad.user.config.JwtProperties;
import com.digitaldad.user.enums.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    public String generateToken(Long userId, UserType userType, String phone) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userType", userType.name())
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public UserType getUserType(String token) {
        return UserType.valueOf(parseToken(token).get("userType", String.class));
    }

    public boolean isExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
