package com.hireflow.hireflow.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    public String generateToken(String subject) {
        return JWT.create()
                .withSubject(subject)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(Algorithm.HMAC256(secret));
    }

    public String extractSubject(String token) {
        return decode(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            decode(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private DecodedJWT decode(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);
    }
}
