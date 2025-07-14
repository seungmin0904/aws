package com.example.boardapi.service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.boardapi.dto.InviteRequestDTO;
import com.example.boardapi.dto.InviteResponseDTO;
import com.example.boardapi.dto.event.ServerMemberEvent;
import com.example.boardapi.entity.ChannelMember;
import com.example.boardapi.entity.ChannelRole;
import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.Invite;
import com.example.boardapi.entity.Member;
import com.example.boardapi.entity.Server;
import com.example.boardapi.entity.ServerMember;
import com.example.boardapi.entity.ServerRole;
import com.example.boardapi.enums.RedisChannelConstants;
import com.example.boardapi.infra.EventPublisher;
import com.example.boardapi.repository.ChannelMemberRepository;
import com.example.boardapi.repository.ChatRoomRepository;
import com.example.boardapi.repository.InviteRepository;
import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.repository.ServerMemberRepository;
import com.example.boardapi.repository.ServerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class InviteService {
    private final ServerRepository serverRepository;
    private final MemberRepository memberRepository;
    private final InviteRepository inviteRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final EventPublisher eventPublisher;

    // 랜덤 코드 생성 유틸
    private String generateRandomCode() {
        // 8자리 영문+숫자 (커스텀 가능)
        return RandomStringUtils.randomAlphanumeric(8);
    }

    public Invite createInvite(Long creatorId, InviteRequestDTO dto) {
        Long serverId = dto.getServerId();
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("서버 없음"));

        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 중복 방지, 충돌날 때 재시도
        String code;
        do {
            code = generateRandomCode();
        } while (inviteRepository.findByCodeAndActiveTrue(code).isPresent());

        Invite invite = Invite.builder()
                .code(code)
                .server(server)
                .creator(creator)
                .expireAt(dto.getExpireAt())
                .maxUses(dto.getMaxUses())
                .memo(dto.getMemo())
                .build();

        return inviteRepository.save(invite);
    }

    public InviteResponseDTO getInviteInfo(String inviteCode) {
        Invite invite = inviteRepository.findByCodeAndActiveTrue(inviteCode)
                .orElseThrow(() -> new NoSuchElementException("초대코드 없음"));

        // 만료 체크
        if (invite.getExpireAt() != null && invite.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("초대코드 만료됨");
        }
        // 횟수 제한 체크
        if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
            throw new IllegalStateException("최대 사용횟수 초과");
        }

        // DTO 변환
        InviteResponseDTO dto = new InviteResponseDTO();
        dto.setInviteCode(invite.getCode());
        dto.setServerId(invite.getServer().getId());
        dto.setServerName(invite.getServer().getName());
        dto.setCreatorName(invite.getCreator().getName());
        dto.setExpireAt(invite.getExpireAt());
        dto.setMaxUses(invite.getMaxUses());
        dto.setUses(invite.getUses());
        dto.setActive(invite.getActive());
        dto.setMemo(invite.getMemo());

        return dto;
    }

    // 초대코드로 방에 참여
    @Transactional
    public Long joinByInvite(String inviteCode, Long memberId) {
        // 1. 초대코드(활성/유효) 검색
        Invite invite = inviteRepository.findByCodeAndActiveTrue(inviteCode)
                .orElseThrow(() -> new NoSuchElementException("초대코드 없음"));

        // 2. 만료/횟수 초과 체크
        if (invite.getExpireAt() != null && invite.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("초대코드 만료됨");
        }
        if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
            throw new IllegalStateException("최대 사용횟수 초과");
        }

        // 3. 방 멤버십 체크 (이미 입장된 사용자면 예외)
        Long serverId = invite.getServer().getId();
        boolean isMember = serverMemberRepository.existsByMemberMnoAndServerId(memberId, serverId);
        if (isMember) {
            throw new IllegalStateException("이미 참여 중인 방입니다.");
        }

        // 4. 방 멤버 등록
        Server server = invite.getServer();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("사용자 없음"));

        ServerMember newMember = ServerMember.builder()
                .server(server)
                .member(member)
                .role(ServerRole.USER) // 이거 반드시 추가!!
                .build();
        serverMemberRepository.save(newMember);

        // 5. 사용횟수 증가, 최대치 도달시 active=false
        invite.setUses(invite.getUses() + 1);
        if (invite.getMaxUses() != null && invite.getUses() >= invite.getMaxUses()) {
            invite.setActive(false);
        }
        inviteRepository.save(invite);
        ServerMemberEvent event = new ServerMemberEvent(serverId, memberId, "JOIN");
        eventPublisher.publishServerMemberEvent(event);

        return server.getId();
    }
}
