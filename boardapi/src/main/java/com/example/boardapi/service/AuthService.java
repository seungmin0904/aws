package com.example.boardapi.service;

import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.security.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtProvider;
    private final MemberRepository memberRepository;

    public void saveRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue().set("user:" + username + ":refresh", refreshToken, Duration.ofDays(14));
    }

    public String reissueAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken))
            return null;

        String username = jwtProvider.getUsername(refreshToken);
        log.info("🔁 리프레시 토큰으로 accessToken 재발급 시도: {}", username);
        String saved = redisTemplate.opsForValue().get("user:" + username + ":refresh");

        if (!refreshToken.equals(saved))
            return null; // 위조된 토큰 거부
        log.info("🔄 AccessToken 재발급 성공 - username: {}", username);
        return memberRepository.findByUsername(username)
                .map(user -> jwtProvider.generateToken(user.getUsername(), user.getName()))
                .orElse(null);
    }

}
