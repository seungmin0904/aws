package com.example.boardapi.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // 이메일

    @Column(unique = true, nullable = false)
    private String token;

    private LocalDateTime expiryDate;
    private boolean verified; // 이메일 인증 여부

    // 토큰 정적 발급 메서드
    // public static EmailVerificationToken create(String username) {
    // return EmailVerificationToken.builder()
    // .username(username)
    // .token(UUID.randomUUID().toString())
    // .expiryDate(LocalDateTime.now().plusMinutes(10)) // 10분 유효
    // .verified(false)
    // .build();
    // }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
