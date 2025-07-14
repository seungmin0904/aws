package com.example.boardapi.service;

import com.example.boardapi.entity.EmailVerificationToken;
import com.example.boardapi.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public EmailVerificationToken sendVerificationCode(String email) {
        tokenRepository.deleteByUsername(email);

        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase(); // 6자리 코드 생성

        EmailVerificationToken token = EmailVerificationToken.builder()
                .username(email)
                .token(code)
                .expiryDate(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .build();

        tokenRepository.save(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[인증] 이메일 확인 코드");
        message.setText("이메일 인증 코드: " + code);
        mailSender.send(message);

        return token;
    }

    public boolean verifyCode(String email, String code) {
        EmailVerificationToken token = tokenRepository
                .findByUsernameAndTokenAndVerifiedFalseAndExpiryDateAfter(email, code, LocalDateTime.now())
                .orElse(null);

        if (token != null) {
            token.setVerified(true);
            tokenRepository.save(token);
            return true;
        }
        return false;
    }
}