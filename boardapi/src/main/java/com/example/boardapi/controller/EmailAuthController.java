package com.example.boardapi.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.boardapi.dto.EmailRequestDTO;
import com.example.boardapi.dto.EmailVerifyDTO;
import com.example.boardapi.entity.EmailVerificationToken;
import com.example.boardapi.entity.Member;
import com.example.boardapi.service.EmailService;
import com.example.boardapi.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailAuthController {
  private final EmailService emailService;
  private final MemberService memberService;

  // 인증코드 전송 요청
  @PostMapping("/send")
  public ResponseEntity<Map<String, Object>> sendCode(@RequestBody EmailRequestDTO dto) {
    EmailVerificationToken token = emailService.sendVerificationCode(dto.getUsername());
    Map<String, Object> response = new HashMap<>();
    response.put("message", "인증코드 전송");
    response.put("expiryDate", token.getExpiryDate());

    return ResponseEntity.ok(response);
  }

  // 인증코드 검증 요청
  @PostMapping("/verify")
  public ResponseEntity<String> verifyCode(@RequestBody EmailVerifyDTO dto) {
    boolean result = emailService.verifyCode(dto.getUsername(), dto.getCode());
    return result
        ? ResponseEntity.ok("인증 성공")
        : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 실패");
  }

  @GetMapping("/find-id")
  public ResponseEntity<String> findName(@RequestParam String username) {
     Optional<String> member = memberService.findByNickname(username);
   if (member.isPresent()) {
        return ResponseEntity.ok(member.get());
    } else {
        return ResponseEntity.notFound().build();
    }
  }
}
