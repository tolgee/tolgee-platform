package com.polygloat.security;

import com.polygloat.constants.Message;
import com.polygloat.exceptions.AuthenticationException;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.UserAccount;
import com.polygloat.service.UserAccountService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Component
public class JwtTokenProvider {

    private JwtProperties properties;
    private UserAccountService userAccountService;

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Autowired
    public JwtTokenProvider(JwtProperties properties, UserAccountService userAccountService) {
        this.properties = properties;
        this.userAccountService = userAccountService;
    }

    public JwtToken generateToken(Long userId) {
        Date now = new Date();

        return new JwtToken(Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(now.getTime() + properties.getJwtExpirationInMs()))
                .signWith(properties.getKey())
                .compact(), properties.getKey());
    }

    public boolean validateToken(JwtToken authToken) {
        try {
            Jwts.parser().setSigningKey(properties.getKey()).parseClaimsJws(authToken.toString());
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }

    public Authentication getAuthentication(JwtToken token) {
        UserAccount userDetails = userAccountService.get(token.getId()).orElseThrow(() -> new AuthenticationException(Message.USER_NOT_FOUND));

        List<GrantedAuthority> authorities = new LinkedList<>();

        GrantedAuthority grantedAuthority = () -> "user";

        authorities.add(grantedAuthority);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public JwtToken resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return new JwtToken(bearerToken.substring(7), properties.getKey());
        }
        return null;
    }
}
