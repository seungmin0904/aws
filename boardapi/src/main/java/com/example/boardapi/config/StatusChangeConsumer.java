package com.example.boardapi.config;

import com.example.boardapi.dto.event.FriendEvent;
import com.example.boardapi.dto.event.StatusChangeEvent;
import com.example.boardapi.entity.Member;
import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.service.FriendService;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StatusChangeConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final FriendService friendService;
    private final MemberRepository memberRepository;

    // // 1. Presence 상태 변경 이벤트 처리
    // @RabbitListener(queues = "presence.queue")
    // public void handleStatusChange(StatusChangeEvent event) {
    // List<String> friendUsernames =
    // friendService.getFriendUsernames(event.getUsername());
    // for (String friend : friendUsernames) {
    // messagingTemplate.convertAndSendToUser(friend, "/queue/status", event);
    // }
    // }

    // 2. 친구 요청/수락/삭제 등 이벤트 처리
    @RabbitListener(queues = "#{friendEventQueue.name}")
    public void handleFriendEvent(FriendEvent event) {
        Member target = memberRepository.findById(event.getTargetUserId()).orElse(null);
        if (target == null)
            return;

        messagingTemplate.convertAndSendToUser(
                target.getUsername(),
                "/queue/friend-events",
                event);
    }
}
