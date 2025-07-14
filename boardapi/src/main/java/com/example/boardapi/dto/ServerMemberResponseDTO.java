package com.example.boardapi.dto;

import com.example.boardapi.entity.ServerMember;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerMemberResponseDTO {
    private Long id; // 참여자 ID
    private String name; // 참여자 이름
    private String profile; // 프로필 이미지
    private String role; // "ADMIN", "USER"

    public static ServerMemberResponseDTO from(ServerMember entity) {
        return ServerMemberResponseDTO.builder()
                .id(entity.getMember().getMno()) // 실제 참여자 ID
                .name(entity.getMember().getName()) // 참여자 이름
                .profile(entity.getMember().getProfile()) // 프로필
                .role(entity.getRole().name()) // "ADMIN" or "USER"
                .build();
    }

}