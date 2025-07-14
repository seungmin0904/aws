package com.example.boardapi.controller;

import com.example.boardapi.dto.ChatRoomResponseDTO;
import com.example.boardapi.entity.ChannelType;
import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.ChatRoomType;
import com.example.boardapi.security.custom.GlobalExceptionHandler;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor

public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채널 생성
    @PostMapping
    public ChatRoomResponseDTO createRoom(
            @AuthenticationPrincipal MemberSecurityDTO member,
            @RequestBody Map<String, String> req) {

        String name = req.get("name");
        String description = req.get("description");
        ChannelType type = ChannelType.valueOf(req.getOrDefault("type", "TEXT"));
        ChatRoomType roomType = ChatRoomType.valueOf(req.get("roomType"));

        Long serverId = null;

        // 서버 채널일 때만 파싱
        if (roomType == ChatRoomType.SERVER && req.get("serverId") != null && !req.get("serverId").isEmpty()) {
            serverId = Long.valueOf(req.get("serverId"));
        }

        return chatRoomService.createRoom(serverId, member.getMno(), name, description, type, roomType);
    }

    // 채널 목록 렌더링
    @GetMapping
    public List<ChatRoom> listRooms() {
        return chatRoomService.listRooms();
    }

    // 채널 삭제 (방장만 가능)
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal MemberSecurityDTO member) {

        chatRoomService.deleteRoom(roomId, member.getMno());
        return ResponseEntity.noContent().build();
    }

}
