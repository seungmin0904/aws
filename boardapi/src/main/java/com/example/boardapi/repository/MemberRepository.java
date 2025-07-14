package com.example.boardapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.boardapi.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // username(email)으로 회원 조회 (로그인, 이메일 중복 확인 등에 사용)
    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username); // 중복 체크용

    Optional<Member> findByname(String name); // 마이페이지 닉네임 변경용

    boolean existsByname(String name);

    // 완전 일치 검색
    List<Member> findAllByName(String name);

    // 부분 일치 검색
    List<Member> findByNameContainingIgnoreCase(String name);

    @Query("SELECT m.username FROM Member m WHERE m.id = :id")
    String findUsernameById(@Param("id") Long id);
}