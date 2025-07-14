package com.example.boardapi.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.boardapi.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
  Optional<EmailVerificationToken> findByUsernameAndTokenAndVerifiedFalseAndExpiryDateAfter(
      String username, String token, LocalDateTime now);

  Optional<EmailVerificationToken> findByUsernameAndVerifiedTrue(String username);

  @Modifying
  @Transactional
  @Query("DELETE FROM EmailVerificationToken t WHERE t.username = :username")
  void deleteByUsername(String username);

  @Modifying
  @Transactional
  void deleteByExpiryDateBefore(LocalDateTime time);
}
