package com.example.boardapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequestDTO {

    @NotBlank
    private String username; // 이메일
    @NotBlank
    private String password;
    @NotBlank
    private String name;
}
