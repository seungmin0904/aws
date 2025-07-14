package com.example.boardapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    // 웹소켓 송수신용 DTO
    private String type; // "ENTER", "CHAT"
    private Long roomId; // 채팅방 번호
    private String sender; // 보내는 사람 (닉네임)
    private String message; // 메시지 내용

}
