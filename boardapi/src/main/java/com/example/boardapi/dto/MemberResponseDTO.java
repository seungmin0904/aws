package com.example.boardapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDTO {
    private Long mno;
    private String username;
    private String name;
    private Long memberId;
}
