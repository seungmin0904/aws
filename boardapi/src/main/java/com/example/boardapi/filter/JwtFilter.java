package com.example.boardapi.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.boardapi.security.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 퍼밋 경로는 토큰 검사 생략
        if ("/api/members/login".equals(uri) || "/api/members/register".equals(uri) || "/auth/refresh".equals(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                setAuth(auth, request);
            } else {
                // 엑세스 토큰이 만료된 경우 리프레시 시도
                String refreshHeader = request.getHeader("Authorization-Refresh");

                if (refreshHeader != null && refreshHeader.startsWith("Bearer ")) {
                    String refreshToken = refreshHeader.substring(7);

                    if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
                        // 새 엑세스 토큰 발급
                        String newAccessToken = jwtTokenProvider.generateAccessTokenFromRefresh(refreshToken);
                        Authentication auth = jwtTokenProvider.getAuthentication(newAccessToken);
                        setAuth(auth, request);

                        // 응답 헤더에 새 엑세스 토큰 추가
                        response.setHeader("Authorization", "Bearer " + newAccessToken);
                    } else {
                        log.warn("❌ Invalid Refresh Token for URI {}: {}", uri, refreshToken);
                    }
                } else {
                    log.warn("❌ Invalid or expired JWT for URI {}: {}", uri, token);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuth(Authentication auth, HttpServletRequest request) {
        if (auth instanceof AbstractAuthenticationToken aat) {
            aat.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(aat);
        } else {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }
}