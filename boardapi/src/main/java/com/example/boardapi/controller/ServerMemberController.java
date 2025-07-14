package com.example.boardapi.controller;

import com.example.boardapi.dto.ServerMemberResponseDTO;
import com.example.boardapi.entity.ServerRole;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.service.ServerMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/members")
@RequiredArgsConstructor
public class ServerMemberController {

    private final ServerMemberService serverMemberService;

    // 서버 참여자 목록 조회
    @GetMapping
    public ResponseEntity<List<ServerMemberResponseDTO>> getServerMembers(@PathVariable Long serverId) {
        return ResponseEntity.ok(serverMemberService.getServerMembers(serverId));
    }

    // 강퇴
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeServerMember(
            @PathVariable Long serverId,
            @PathVariable Long memberId) {
        serverMemberService.removeServerMember(serverId, memberId);
        return ResponseEntity.ok().build();
    }

    // 권한 변경
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> changeServerMemberRole(
            @PathVariable Long serverId,
            @PathVariable Long memberId,
            @RequestBody String newRole // "ADMIN", "USER" 등
    ) {
        serverMemberService.changeServerMemberRole(serverId, memberId, ServerRole.valueOf(newRole));
        return ResponseEntity.ok().build();
    }

    // (선택) 참여자 권한 조회
    @GetMapping("/{memberId}/role")
    public ResponseEntity<String> getMemberRole(
            @PathVariable Long serverId,
            @PathVariable Long memberId) {
        String role = serverMemberService.getMemberRole(serverId, memberId);
        if (role == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(role);
    }

    // 서버 탈퇴
    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveServer(
            @PathVariable Long serverId,
            @AuthenticationPrincipal MemberSecurityDTO principal) {
        serverMemberService.leaveServer(serverId, principal.getMno());
        return ResponseEntity.ok("서버에서 탈퇴했습니다.");
    }
}