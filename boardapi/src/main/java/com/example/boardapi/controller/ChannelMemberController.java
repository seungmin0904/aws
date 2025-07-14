package com.example.boardapi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.boardapi.dto.ChannelMemberResponseDTO;
import com.example.boardapi.entity.ChannelMember;
import com.example.boardapi.entity.ChannelRole;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.service.ChannelMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/channel-members")
@RequiredArgsConstructor
public class ChannelMemberController {

    private final ChannelMemberService channelMemberService;

    // 채널 참여 (입장)
    @PostMapping("/join")
    public ChannelMember joinChannel(
            @AuthenticationPrincipal MemberSecurityDTO member, // JWT 인증 유저
            @RequestBody Map<String, Object> req) {
        Long roomId = Long.valueOf(req.get("roomId").toString());
        ChannelRole role = ChannelRole.USER; // 일반 사용자는 USER로 고정
        return channelMemberService.joinChannel(member.getMno(), roomId, role);
    }

    // 채널별 참여자 목록 조회
    @GetMapping("/room/{roomId}")
    public List<ChannelMemberResponseDTO> listMembers(@PathVariable Long roomId) {
        return channelMemberService.listMembers(roomId);
    }
}
