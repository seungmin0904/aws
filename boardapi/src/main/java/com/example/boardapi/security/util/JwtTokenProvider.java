package com.example.boardapi.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.example.boardapi.security.custom.CustomUserDetailsService;
import com.example.boardapi.security.dto.MemberSecurityDTO;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long validityInMilliseconds;

    private Key key;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtTokenProvider(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostConstruct
    public void init() {
        // Base64 디코딩 대신 UTF-8 바이트 사용
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);

    }

    public String generateToken(String username, String name) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);
        if (username == null || name == null) {
            log.error("❌ 토큰 생성 실패: username={}, name={}", username, name);
            throw new IllegalArgumentException("토큰 생성에 필요한 정보가 부족합니다");
        }
        return Jwts.builder()
                .setSubject(username)
                .claim("name", name)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + Duration.ofDays(14).toMillis());

        String refreshToken = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("🔐 RefreshToken 생성 - username: {}, 만료: {}, token: {}",
                username, expiry, refreshToken);

        return refreshToken;
    }

    public Authentication getAuthentication(String token) {
        String username = getUsername(token);
        var userDetails = customUserDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String validateAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            log.warn("Cannot parse JWT subject: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        return validateToken(refreshToken); // 내부적으로 유효성 검증
    }

    public String generateAccessTokenFromRefresh(String refreshToken) {
        String username = getUsername(refreshToken);
        return generateToken(username,
                ((MemberSecurityDTO) customUserDetailsService.loadUserByUsername(username)).getName());
    }
}
