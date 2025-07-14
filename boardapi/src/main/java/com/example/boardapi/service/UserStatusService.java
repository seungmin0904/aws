package com.example.boardapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.example.boardapi.entity.Friend;
import com.example.boardapi.entity.FriendStatus;
import com.example.boardapi.entity.Member;
import com.example.boardapi.enums.UserStatus;
import com.example.boardapi.infra.EventPublisher;
import com.example.boardapi.repository.FriendRepository;
import com.example.boardapi.repository.MemberRepository;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatusService {

        private final RedisTemplate<String, String> redisTemplate;
        private final EventPublisher eventPublisher;
        private final FriendRepository friendRepository;
        private final MemberRepository memberRepository;

        public void markOnline(String username, String sessionId) {
                String sessionKey = "user:" + username + ":sessions";

                Set<String> oldSessions = redisTemplate.opsForSet().members(sessionKey);
                if (oldSessions != null) {
                        for (String oldSession : oldSessions) {
                                redisTemplate.opsForSet().remove(sessionKey, oldSession);
                                log.info("♻️ 재연결: 이전 세션 {} 제거됨", oldSession);
                        }
                }

                redisTemplate.opsForSet().add(sessionKey, sessionId);
                redisTemplate.opsForSet().add("online_users", username);
                log.info("✅ 최종 세션 등록: user={}, sessionId={}", username, sessionId);

                if (oldSessions == null || oldSessions.isEmpty()) {
                        List<String> friendUsernames = getFriendUsernames(username);
                        eventPublisher.publishOnline(username, friendUsernames);
                }
        }

        public void markOffline(String username, String sessionId) {
                String sessionsKey = "user:" + username + ":sessions";
                redisTemplate.opsForSet().remove(sessionsKey, sessionId);
                Long remaining = redisTemplate.opsForSet().size(sessionsKey);

                log.info("❌ Disconnect: user={}, sessionId={}, remaining={}", username, sessionId, remaining);

                if (remaining == null || remaining == 0) {
                        redisTemplate.delete(sessionsKey);
                        redisTemplate.opsForSet().remove("online_users", username);

                        List<String> friendUsernames = getFriendUsernames(username);
                        eventPublisher.publishOffline(username, friendUsernames);
                }
        }

        public List<String> getOnlineFriendUsernames(String myUsername) {
                Long myId = memberRepository.findByUsername(myUsername)
                                .orElseThrow(() -> new UsernameNotFoundException(myUsername))
                                .getMno();

                List<String> allFriends = friendRepository.findFriendUsernamesByStatusAndMyId(FriendStatus.ACCEPTED,
                                myId);
                Set<String> online = redisTemplate.opsForSet().members("online_users");
                return allFriends.stream().filter(online::contains).collect(Collectors.toList());
        }

        public List<String> getFriendUsernames(String myUsername) {
                Long myId = memberRepository.findByUsername(myUsername)
                                .orElseThrow(() -> new UsernameNotFoundException(myUsername))
                                .getMno();

                return friendRepository.findFriendUsernamesByStatusAndMyId(FriendStatus.ACCEPTED, myId);
        }

        // 서버 실행 시 online_users, session 초기화

        @PostConstruct
        public void clearOnlineUsersAtStartup() {

                redisTemplate.delete("online_users");
                // 모든 세션 키 삭제
                Set<String> keys = redisTemplate.keys("user:*:sessions");
                if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                }

                log.info("🧹 Redis 초기화: online_users 및 user:*:sessions, user:*:refresh 삭제 완료");
        }
}