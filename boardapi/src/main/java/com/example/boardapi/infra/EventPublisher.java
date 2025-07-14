package com.example.boardapi.infra;

import com.example.boardapi.dto.event.FriendEvent;
import com.example.boardapi.dto.event.ServerMemberEvent;
import com.example.boardapi.dto.event.StatusChangeEvent;
import com.example.boardapi.enums.RedisChannelConstants;
import com.example.boardapi.enums.UserStatus;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Qualifier("EventRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    public void publishOnline(String username, Iterable<String> friendUsernames) {
        StatusChangeEvent event = new StatusChangeEvent(username, UserStatus.ONLINE);
        redisTemplate.convertAndSend("status.change", event);
        messagingTemplate.convertAndSend("/topic/online-users", event);
        for (String friend : friendUsernames) {
            messagingTemplate.convertAndSendToUser(friend, "/queue/status",
                    Map.of("username", username, "status", "ONLINE"));
        }
    }

    public void publishOffline(String username, Iterable<String> friendUsernames) {
        StatusChangeEvent event = new StatusChangeEvent(username, UserStatus.OFFLINE);
        redisTemplate.convertAndSend("status.change", event);
        messagingTemplate.convertAndSend("/topic/online-users", event);
        for (String friend : friendUsernames) {
            messagingTemplate.convertAndSendToUser(friend, "/queue/status",
                    Map.of("username", username, "status", "OFFLINE"));
        }
    }

    public void publishFriendEvent(FriendEvent event, String username) {
        redisTemplate.convertAndSend(RedisChannelConstants.FRIEND_REQUEST_CHANNEL, event);
        messagingTemplate.convertAndSendToUser(username, "/queue/friend", event);
    }

    public void publishServerMemberEvent(ServerMemberEvent event) {
        redisTemplate.convertAndSend(RedisChannelConstants.SERVER_MEMBER_CHANGE, event);
        // WebSocket으로 참여자에게 실시간 전송
        messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getMemberId()),
                "/queue/server",
                event);
    }

    public void publishServerChange(Object event, Long targetUserId) {
        redisTemplate.convertAndSend(RedisChannelConstants.SERVER_CHANGE, event);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(targetUserId),
                "/queue/server-list",
                event);
    }

    public void publishInviteEvent(Object event, Long targetUserId) {
        redisTemplate.convertAndSend(RedisChannelConstants.INVITE_CHANGE, event);
    }
}