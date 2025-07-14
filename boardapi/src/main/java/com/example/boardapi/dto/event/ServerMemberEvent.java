package com.example.boardapi.dto.event;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerMemberEvent {

    private Long serverId;
    private Long memberId; // 탈퇴/참여한 멤버
    private String type; // JOIN, LEAVE, DELETE

}
