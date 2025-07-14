package com.example.boardapi.service;

import com.example.boardapi.dto.ChatRoomResponseDTO;
import com.example.boardapi.dto.ServerMemberResponseDTO;
import com.example.boardapi.dto.ServerRequestDTO;
import com.example.boardapi.dto.ServerResponseDTO;
import com.example.boardapi.dto.event.ServerMemberEvent;
import com.example.boardapi.entity.Server;
import com.example.boardapi.entity.ChatMessageEntity;
import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.ChatRoomMember;
import com.example.boardapi.entity.Member;
import com.example.boardapi.entity.ServerMember;
import com.example.boardapi.entity.ServerRole;
import com.example.boardapi.enums.RedisChannelConstants;
import com.example.boardapi.infra.EventPublisher;
import com.example.boardapi.repository.ServerRepository;
import com.example.boardapi.repository.ServerMemberRepository;
import com.example.boardapi.repository.ChatMessageRepository;
import com.example.boardapi.repository.ChatRoomMemberRepository;
import com.example.boardapi.repository.ChatRoomRepository;
import com.example.boardapi.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServerService {

        private final ServerRepository serverRepository;
        private final ServerMemberRepository serverMemberRepository;
        private final MemberRepository memberRepository;
        private final ChatRoomRepository chatRoomRepository;
        private final ChatMessageRepository chatMessageRepository;
        private final ChatRoomMemberRepository chatRoomMemberRepository;
        private final EventPublisher eventPublisher;

        @Transactional
        public ServerResponseDTO createServer(ServerRequestDTO dto, Long ownerId) {
                Member owner = memberRepository.findById(ownerId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

                Server server = Server.builder()
                                .name(dto.getName())
                                .description(dto.getDescription())
                                .build();
                serverRepository.save(server);

                // 서버 개설자에게 ADMIN 권한 부여
                ServerMember serverMember = ServerMember.builder()
                                .member(owner)
                                .server(server)
                                .role(ServerRole.ADMIN)
                                .build();
                serverMemberRepository.save(serverMember);

                return ServerResponseDTO.from(server);
        }

        @Transactional(readOnly = true)
        public List<ServerResponseDTO> getAllServers(Long memberId) {
                // "내가 참여한 서버"만 보여주려면 아래처럼
                List<ServerMember> joined = serverMemberRepository.findByMemberMno(memberId);

                return joined.stream()
                                .map(sm -> ServerResponseDTO.from(sm.getServer(), sm.getRole()))
                                .collect(Collectors.toList());
        }

        @Transactional
        public void deleteServer(Long serverId, Long memberId) {
                Server server = serverRepository.findById(serverId)
                                .orElseThrow(() -> new IllegalArgumentException("서버 없음"));

                // 개설자(Owner)만 삭제 허용
                ServerMember ownerMember = serverMemberRepository
                                .findByServerIdAndRole(serverId, ServerRole.ADMIN)
                                .orElseThrow(() -> new IllegalAccessError("서버 오너가 존재하지 않습니다."));

                if (!ownerMember.getMember().getMno().equals(memberId)) {
                        throw new IllegalAccessError("서버 삭제 권한이 없습니다.");
                }
                // 💡 Redis 이벤트용: 구성원 ID 목록 수집
                List<Long> memberIds = server.getMembers().stream()
                                .map(m -> m.getMember().getMno())
                                .collect(Collectors.toList());

                // 연관 엔티티들 cascade+orphanRemoval 로 자동 삭제되도록 JPA에 인식시키기

                // 1. 채널 순회
                for (ChatRoom room : server.getChannels()) {
                        // 1-1. 메시지 삭제
                        chatMessageRepository.deleteAll(room.getMessages());
                        // 1-2. 채팅방 참여자 삭제
                        chatRoomMemberRepository.deleteAll(room.getMembers());
                }
                // 3. 채널 삭제
                chatRoomRepository.deleteAll(server.getChannels());
                // 2. 서버 구성원 삭제
                serverMemberRepository.deleteAll(server.getMembers());
                // 4. 서버 삭제
                serverRepository.delete(server);

                for (Long targetId : memberIds) {
                        ServerMemberEvent event = new ServerMemberEvent(serverId, targetId, "DELETE");
                        eventPublisher.publishServerMemberEvent(event);
                }

        }

        // 서버 검색
        @Transactional(readOnly = true)
        public List<ServerResponseDTO> searchServers(String keyword) {
                List<Server> servers;
                if (keyword == null || keyword.isBlank()) {
                        servers = serverRepository.findAll();
                } else {
                        servers = serverRepository.findByNameContainingIgnoreCase(keyword);
                }
                return servers.stream().map(ServerResponseDTO::from).collect(Collectors.toList());
        }

        // 서버별 채널 목록 조회
        public List<ChatRoomResponseDTO> getChannelsByServer(Long serverId) {
                Server server = serverRepository.findById(serverId)
                                .orElseThrow(() -> new IllegalArgumentException("서버 없음"));
                List<ChatRoom> channels = chatRoomRepository.findByServer(server);
                return channels.stream()
                                .map(room -> {
                                        // DTO에 서버 정보도 추가해서 반환
                                        ChatRoomResponseDTO dto = ChatRoomResponseDTO.from(room);
                                        dto.setServerId(server.getId());
                                        dto.setServerName(server.getName());
                                        // 필요시 ownerName 등 추가
                                        return dto;
                                })
                                .toList();
        }

}
