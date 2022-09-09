package io.tolgee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.util.Date;


public class JwtToken {
    private final String value;

    private final Jws<Claims> parsed;

    public JwtToken(String value, Key key) {
        this.value = value;
        this.parsed = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(value);
    }

    public String getContent() {
        return this.parsed.getBody().getSubject();
    }

    public Long getId() {
        return Long.parseLong(getContent());
    }

    public Date getIssuedAt() {
        return this.parsed.getBody().getIssuedAt();
    }

    @Override
    public String toString() {
        return this.value;
    }
}
