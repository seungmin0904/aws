package com.example.boardapi.filter;

import java.io.IOException;
import java.util.Set;

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

    private static final Set<String> PUBLIC_URIS = Set.of(
            "/api/members/login",
            "/api/members/register",
            "/api/members/check-nickname",
            "/api/members/find-id",
            "/api/members/password/reset",
            "/api/auth/refresh",
            "/api/auth/email/send",
            "/api/auth/email/verify",
            "/api/auth/email/find-id",
            "/error");

    private boolean isPublicUri(String uri) {
        // 정확히 일치 또는 시작 경로가 PUBLIC_URIS에 포함되면 허용
        return PUBLIC_URIS.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.startsWith("/ws-chat")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublicUri(uri)) {
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
                        String newAccessToken = jwtTokenProvider.generateAccessTokenFromRefresh(refreshToken);
                        Authentication auth = jwtTokenProvider.getAuthentication(newAccessToken);
                        setAuth(auth, request);
                        response.setHeader("Authorization", "Bearer " + newAccessToken);
                    } else {
                        log.warn("❌ Invalid Refresh Token for URI {}: {}", uri, refreshToken);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                } else {
                    log.warn("❌ Invalid or expired JWT for URI {}: {}", uri, token);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        } else {
            // ✅ Authorization 헤더가 없고, 보호된 경로일 경우 -> 명시적으로 401 처리
            log.warn("❌ Missing Authorization header for protected URI {}", uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
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
