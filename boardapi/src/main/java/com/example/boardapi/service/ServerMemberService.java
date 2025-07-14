package com.example.boardapi.service;

import com.example.boardapi.dto.ServerMemberResponseDTO;
import com.example.boardapi.dto.event.ServerMemberEvent;
import com.example.boardapi.entity.*;
import com.example.boardapi.infra.EventPublisher;
import com.example.boardapi.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServerMemberService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final MemberRepository memberRepository;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ServerMemberResponseDTO> getServerMembers(Long serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("서버 없음"));
        List<ServerMember> list = serverMemberRepository.findByServer(server);
        return list.stream().map(ServerMemberResponseDTO::from).toList();
    }

    @Transactional
    public void removeServerMember(Long serverId, Long memberId) {
        ServerMember serverMember = serverMemberRepository.findByMemberMnoAndServerId(memberId, serverId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 서버에 없음"));
        serverMemberRepository.delete(serverMember);

        // 실시간 퇴장 전파
        ServerMemberEvent event = new ServerMemberEvent(serverId, memberId, "KICK");
        eventPublisher.publishServerMemberEvent(event);
    }

    @Transactional
    public void changeServerMemberRole(Long serverId, Long memberId, ServerRole newRole) {
        ServerMember serverMember = serverMemberRepository.findByMemberMnoAndServerId(memberId, serverId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 서버에 없음"));
        serverMember.setRole(newRole);
        serverMemberRepository.save(serverMember);
        // 권한 변경 추가예정
    }

    @Transactional
    public void joinServer(Long serverId, Long memberId) {
        boolean exists = serverMemberRepository.existsByMemberMnoAndServerId(memberId, serverId);
        if (exists)
            throw new IllegalStateException("이미 참여 중");

        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("서버 없음"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        ServerMember serverMember = ServerMember.builder()
                .server(server)
                .member(member)
                .role(ServerRole.USER)
                .build();
        serverMemberRepository.save(serverMember);

        // 실시간 입장 전파
        ServerMemberEvent event = new ServerMemberEvent(serverId, memberId, "JOIN");
        eventPublisher.publishServerMemberEvent(event);
    }

    @Transactional(readOnly = true)
    public String getMemberRole(Long serverId, Long memberId) {
        Optional<ServerMember> sm = serverMemberRepository.findByMemberMnoAndServerId(memberId, serverId);
        return sm.map(serverMember -> serverMember.getRole().name()).orElse(null);
    }

    @Transactional
    public void leaveServer(Long serverId, Long memberId) {
        ServerMember serverMember = serverMemberRepository
                .findByMemberMnoAndServerId(memberId, serverId)
                .orElseThrow(() -> new IllegalArgumentException("서버 참여 정보가 없습니다."));
        serverMemberRepository.delete(serverMember);

        // 실시간 퇴장 전파
        ServerMemberEvent event = new ServerMemberEvent(serverId, memberId, "LEAVE");
        eventPublisher.publishServerMemberEvent(event);
    }
}
