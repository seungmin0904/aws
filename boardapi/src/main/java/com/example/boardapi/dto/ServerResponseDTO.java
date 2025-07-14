package com.example.boardapi.dto;

import com.example.boardapi.entity.Server;
import com.example.boardapi.entity.ServerRole;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String role;

    // 역할 있음
    public static ServerResponseDTO from(Server entity, ServerRole role) {
        return ServerResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .role(role != null ? role.name() : null)
                .description(entity.getDescription())
                .build();
    }

    // 역할 없음
    public static ServerResponseDTO from(Server server) {
        return from(server, null);
    }
}
