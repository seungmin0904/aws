package com.example.boardapi.listener;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.boardapi.service.UserStatusService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    // WebSocket 연결 상태를 Redis에 저장하고 사용자 온라인/오프라인 상태를 관리
    private final @Qualifier("redisTemplate") RedisTemplate<String, String> redis;
    private final UserStatusService userStatusService;

    // 연결 시
    @EventListener
    public void onConnected(SessionConnectedEvent ev) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(ev.getMessage());
        String user = sha.getUser().getName();
        String sessionId = sha.getSessionId();

        userStatusService.markOnline(user, sessionId); // 위임

    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent ev) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(ev.getMessage());
        String sessionId = sha.getSessionId();
        Principal principal = sha.getUser();

        if (principal != null) {
            String username = principal.getName();
            userStatusService.markOffline(username, sessionId);
            return;
        }

        // 💡 fallback: sessionAttributes에서 username 수동 복원
        Map<String, Object> sessionAttributes = sha.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
            String username = (String) sessionAttributes.get("username");
            userStatusService.markOffline(username, sessionId);
            return;
        }
    }
}