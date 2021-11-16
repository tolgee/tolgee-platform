package io.tolgee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.Key;


public class JwtToken {
    private final String value;
    private final Key key;

    public JwtToken(String value, Key key) {
        this.value = value;
        this.key = key;
    }

    public String getContent() {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(this.value).getBody();
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
