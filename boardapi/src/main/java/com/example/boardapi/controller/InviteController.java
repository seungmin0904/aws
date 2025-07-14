package com.example.boardapi.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.boardapi.dto.InviteRequestDTO;
import com.example.boardapi.dto.InviteResponseDTO;
import com.example.boardapi.entity.Invite;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.service.InviteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor

public class InviteController {

    private final InviteService inviteService;

    // 초대코드 생성 (방 소유자/멤버만)
    @PostMapping
    public ResponseEntity<?> createInvite(
            @RequestBody InviteRequestDTO dto,
            @AuthenticationPrincipal MemberSecurityDTO user // JWT 인증
    ) {
        log.info("📩 초대코드 생성 요청: userId={}, serverId={}, expireAt={}, maxUses={}, memo={}",
                user.getMno(), dto.getServerId(), dto.getExpireAt(), dto.getMaxUses(), dto.getMemo());

        try {
            Invite invite = inviteService.createInvite(user.getMno(), dto);

            log.info("✅ 초대코드 생성 성공: code={}, serverId={}, createdBy={}",
                    invite.getCode(), invite.getServer().getId(), invite.getCreator().getMno());

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("inviteCode", invite.getCode());
            response.put("expireAt", invite.getExpireAt());
            response.put("maxUses", invite.getMaxUses());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("❌ 초대코드 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("초대코드 생성 중 오류 발생: " + e.getMessage());
        }
    }

    // 초대코드로 정보 조회
    @GetMapping("/{inviteCode}")
    public ResponseEntity<?> getInvite(
            @PathVariable String inviteCode) {
        try {
            InviteResponseDTO dto = inviteService.getInviteInfo(inviteCode);
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("초대코드를 찾을 수 없습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(410).body(e.getMessage()); // 410 Gone: 만료/초과 등
        }
    }

    // 초대코드로 채널 참여
    @PostMapping("/{inviteCode}/join")
    public ResponseEntity<?> joinByInvite(
            @PathVariable String inviteCode,
            @AuthenticationPrincipal MemberSecurityDTO user) {
        try {
            Long serverId = inviteService.joinByInvite(inviteCode, user.getMno());
            return ResponseEntity.ok(Map.of("serverId", serverId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body("초대코드를 찾을 수 없습니다.");
        } catch (IllegalStateException e) {
            // 만료, 초과, 이미멤버 등
            return ResponseEntity.status(410).body(e.getMessage());
        }
    }

}
