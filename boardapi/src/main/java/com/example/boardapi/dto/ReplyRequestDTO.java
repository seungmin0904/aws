package com.example.boardapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyRequestDTO {
    private Long bno;
    private Long parentRno;
    private String text;
    private String username; // 현재 로그인 사용자 (작성자 식별용)
}
