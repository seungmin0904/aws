package com.example.boardapi.config;

import com.example.boardapi.entity.Member;
import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.security.util.JwtTokenProvider;
import com.example.boardapi.websocket.StompPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Value("${spring.profiles.active:ec2}")
    private String activeProfile;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = activeProfile.equals("local")
                ? List.of("http://localhost:5173", "http://localhost:5174")
                : List.of("https://serverpro.kro.kr");

        registry
                .addEndpoint("/ws-chat")
                .setAllowedOrigins(origins.toArray(String[]::new));
        // .withSockJS(); ← sockjs 안 쓰면 생략
    }

    // 2) 브로커 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // 3) CONNECT 프레임에 대한 JWT 검사용 ChannelInterceptor 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor());
    }

    @Bean
    public ChannelInterceptor jwtChannelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    try {
                        accessor.setLeaveMutable(true);

                        String bearer = accessor.getFirstNativeHeader("Authorization");
                        if (bearer == null || !bearer.startsWith("Bearer ")) {
                            throw new IllegalArgumentException("Missing or invalid Authorization header");
                        }
                        log.info("Authorization Header: {}", bearer);

                        String token = bearer.substring(7);
                        if (!jwtTokenProvider.validateToken(token)) {
                            throw new IllegalArgumentException("Invalid JWT token");
                        }
                        String username = jwtTokenProvider.validateAndGetUsername(token);
                        String sessionId = accessor.getSessionId();

                        accessor.setUser(new StompPrincipal(username, sessionId));
                        log.info("✅ 연결된 사용자: {}", username, sessionId);
                        accessor.getSessionAttributes().put("username", username);
                        accessor.getSessionAttributes().put("nickname",
                                memberRepository.findByUsername(username)
                                        .map(Member::getName)
                                        .orElse("알 수 없음"));

                    } catch (Exception e) {
                        log.error("❌ STOMP CONNECT 인증 실패: {}", e.getMessage(), e);
                        throw e;
                    }
                }

                return message;
            }
        };
    }
}
