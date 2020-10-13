package com.polygloat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.Key;


public class JwtToken {
    private String value;
    private Key key;

    public JwtToken(String value, Key key) {
        this.value = value;
        this.key = key;
    }

    public String getContent() {
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(this.value)
                .getBody();

        return claims.getSubject();
    }

    @Override
    public String toString() {
        return value;
    }

    public Long getId() {
        return Long.parseLong(getContent());
    }
}
