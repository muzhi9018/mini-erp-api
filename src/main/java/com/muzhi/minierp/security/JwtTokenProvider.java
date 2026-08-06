package com.muzhi.minierp.security;

import com.muzhi.minierp.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;


    public String createToken(LoginUser loginUser) {
        Objects.requireNonNull(loginUser, "loginUser must not be null");

        Instant now = Instant.now();
        Instant expireAt = now.plus(properties.getExpireMinutes(), ChronoUnit.MINUTES);

        JwtBuilder jwtBuilder = Jwts.builder()
                .subject(loginUser.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("userId", loginUser.getId())
                .claim("userNo", loginUser.getUserNo())
                .claim("roleCode", loginUser.getRoleCode())
                .claim("tokenType", loginUser.getTokenType())
                .signWith(this.getSigningKey());

        if (StringUtils.hasText(properties.getIssuer())) {
            jwtBuilder.issuer(properties.getIssuer());
        }

        return jwtBuilder.compact();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            this.parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        LoginUser loginUser = this.getLoginUser(token);
        return new UsernamePasswordAuthenticationToken(
                loginUser,
                null,
                loginUser.getAuthorities()
        );
    }

    public LoginUser getLoginUser(String token) {
        Claims claims = this.parseClaims(token);
        String roleCode = this.getStringClaim(claims, "roleCode");

        LoginUser loginUser = new LoginUser(
                claims.getSubject(),
                "",
                true,
                this.buildAuthorities(roleCode)
        );
        loginUser.setId(this.getLongClaim(claims, "userId"));
        loginUser.setUserNo(this.getStringClaim(claims, "userNo"));
        loginUser.setTokenType(this.getStringClaim(claims, "tokenType"));
        loginUser.setRoleCode(roleCode);
        return loginUser;
    }

    private SecretKey getSigningKey() {
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("security.jwt.secret must not be blank");
        }
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseClaims(String token) {
        var parserBuilder = Jwts.parser()
                .verifyWith(this.getSigningKey());

        if (StringUtils.hasText(properties.getIssuer())) {
            parserBuilder.requireIssuer(properties.getIssuer());
        }

        return parserBuilder
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String getStringClaim(Claims claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : value.toString();
    }

    private Long getLongClaim(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            return Long.valueOf(text);
        }
        return null;
    }

    private Collection<SimpleGrantedAuthority> buildAuthorities(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        String authority = roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

}
