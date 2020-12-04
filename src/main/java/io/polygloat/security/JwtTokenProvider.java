package io.polygloat.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.polygloat.configuration.polygloat.PolygloatProperties;
import io.polygloat.constants.Message;
import io.polygloat.exceptions.AuthenticationException;
import io.polygloat.model.UserAccount;
import io.polygloat.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Component
public class JwtTokenProvider {

    private PolygloatProperties configuration;
    private UserAccountService userAccountService;
    private JwtSecretProvider jwtSecretProvider;

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Autowired
    public JwtTokenProvider(PolygloatProperties configuration, UserAccountService userAccountService, JwtSecretProvider jwtSecretProvider) {
        this.configuration = configuration;
        this.userAccountService = userAccountService;
        this.jwtSecretProvider = jwtSecretProvider;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecretProvider.getJwtSecret());
    }

    public JwtToken generateToken(Long userId) {
        Date now = new Date();

        return new JwtToken(Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(now.getTime() + configuration.getAuthentication().getJwtExpiration()))
                .signWith(getKey())
                .compact(), getKey());
    }

    public boolean validateToken(JwtToken authToken) {
        try {
            Jwts.parser().setSigningKey(getKey()).parseClaimsJws(authToken.toString());
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
            return new JwtToken(bearerToken.substring(7), getKey());
        }
        return null;
    }
}
