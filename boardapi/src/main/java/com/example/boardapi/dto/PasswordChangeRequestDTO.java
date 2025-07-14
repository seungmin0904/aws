package com.example.boardapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequestDTO {
    
    private String currentPassword; // 기존 비밀번호 (마이페이지 변경용)
    private String newPassword;     // 새 비밀번호
    private String email;           // 이메일 (비밀번호 찾기용)
}
