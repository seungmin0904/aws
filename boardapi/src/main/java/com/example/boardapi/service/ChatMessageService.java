package com.example.boardapi.service;

import com.example.boardapi.dto.event.DmRestoreEvent;
import com.example.boardapi.entity.ChatMessageEntity;
import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.ChatRoomMember;
import com.example.boardapi.entity.Member;
import com.example.boardapi.repository.ChatMessageRepository;
import com.example.boardapi.repository.ChatRoomMemberRepository;
import com.example.boardapi.repository.ChatRoomRepository;
import com.example.boardapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

        private final ChatMessageRepository chatMessageRepository;
        private final MemberRepository memberRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final ChatRoomRepository chatRoomRepository;
        private final ChatRoomMemberRepository chatRoomMemberRepository;
        private final ApplicationEventPublisher eventPublisher;

        // 채팅방 메시지 조회
        public List<ChatMessageEntity> getMessagesByRoomId(Long roomId, Long memberId) {
                log.info("🔍 메시지 조회 요청: roomId={}", roomId);
                ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
                LocalDateTime leftAt = chatRoomMemberRepository.findByChatRoomIdAndMemberMno(roomId, memberId)
                                .map(ChatRoomMember::getLeftAt)
                                .orElse(null);

                if (leftAt != null) {
                        return chatMessageRepository.findByRoomAndSentAtAfterOrderBySentAtAsc(chatRoom, leftAt);
                } else {
                        return chatMessageRepository.findByRoomOrderBySentAtAsc(chatRoom);
                }
        }

        @Transactional
        public void handleMessage(Long roomId, String message, String username) {
                log.info("📨 새 메시지 수신: roomId={}, sender={}, message={}", roomId, username, message);

                Member sender = memberRepository.findByUsername(username)
                                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

                ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                                .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

                List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);

                for (ChatRoomMember member : members) {
                        Long memberId = member.getMember().getMno();
                        if (!memberId.equals(sender.getMno())) {
                                if (!member.isVisible()) {
                                        member.setVisible(true);
                                        chatRoomMemberRepository.save(member);
                                        log.info("✅ 숨김 해제 및 visible 복구: memberId={}", memberId);
                                        Hibernate.initialize(member.getMember());
                                        // WebSocket 알림
                                        eventPublisher.publishEvent(
                                                        new DmRestoreEvent(member.getMember().getUsername(), roomId));
                                }
                        }
                }

                ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                                .room(chatRoom)
                                .message(message)
                                .sentAt(LocalDateTime.now())
                                .sender(sender)
                                .build();
                chatMessageRepository.save(chatMessage);

                log.info("✅ 메시지 저장 완료: roomId={}, senderId={}", roomId, sender.getMno());
        }
}