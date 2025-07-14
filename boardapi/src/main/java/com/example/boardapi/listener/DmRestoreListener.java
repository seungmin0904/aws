package com.example.boardapi.listener;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.boardapi.dto.event.DmRestoreEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmRestoreListener {

    private final SimpMessagingTemplate messagingTemplate;

    // redis 없이 WebSocket을 통해 DM 복구 알림 전송
    // @TransactionalEventListener를 사용하여 트랜잭션 커밋 후에
    // redis 사용으로 전환하려면 RedisTemplate(@EventListener)을 주입받아 사용
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRestore(DmRestoreEvent event) {
        String username = event.getUsername();
        Long roomId = event.getRoomId();

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/dm-restore",
                Map.of("roomId", roomId, "status", "RESTORE"));

        log.info("📡 [AFTER_COMMIT] DM 복구 WebSocket 전송 → {}", username);
    }
}
