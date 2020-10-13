package com.polygloat.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtProperties {
    private static final byte[] DEFAULT_JWT = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();

    @Value("${app.jwtSecret:#{null}}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs:604800000}")
    private int jwtExpirationInMs;

    public Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret == null ? DEFAULT_JWT : jwtSecret.getBytes());
    }

    public int getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }
}
