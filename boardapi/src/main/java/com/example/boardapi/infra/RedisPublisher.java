
// package com.example.boardapi.infra;

// import com.example.boardapi.dto.StatusChangeEvent;
// import com.example.boardapi.enums.UserStatus;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.stereotype.Component;

// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class RedisPublisher {

// private final RedisTemplate<String, Object> redisTemplate;

// public void publishOnline(String username) {
// StatusChangeEvent event = new StatusChangeEvent(username, UserStatus.ONLINE);
// redisTemplate.convertAndSend(STATUS_CHANGE_CHANNEL, event);
// log.info("📡 [REDIS] 온라인 이벤트 발행: {}", event);
// }

// public void publishOffline(String username) {
// StatusChangeEvent event = new StatusChangeEvent(username,
// UserStatus.OFFLINE);
// redisTemplate.convertAndSend(STATUS_CHANGE_CHANNEL, event);
// log.info("📡 [REDIS] 오프라인 이벤트 발행: {}", event);
// }
// }
