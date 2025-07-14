package com.example.boardapi.service;

import com.example.boardapi.dto.LoginRequestDTO;
import com.example.boardapi.dto.MemberRequestDTO;
import com.example.boardapi.dto.MemberResponseDTO;
import com.example.boardapi.entity.Member;
import com.example.boardapi.mapper.MemberMapper;
import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.security.dto.MemberSecurityDTO;
import com.example.boardapi.security.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;

    // 회원가입: 이메일(=username)로 중복 체크, name(아이디/닉네임)은 별도 중복 체크
    public MemberResponseDTO register(MemberRequestDTO dto) {
        if (memberRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByname(dto.getName())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
        Member member = MemberMapper.toEntity(dto);
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        Member saved = memberRepository.save(member);
        return MemberMapper.toDTO(saved);
    }

    // 로그인: username(email) + password
    public Member login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        MemberSecurityDTO principal = (MemberSecurityDTO) authentication.getPrincipal();

        // DB에서 username(닉네임)으로 조회
        return memberRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("회원 정보 없음"));
    }

    // 로그아웃 (Redis에서 refresh 토큰 삭제)
    public void logout(String username) {
        String key = "user:" + username + ":refresh";
        redisTemplate.delete(key);
        log.info("🔓 로그아웃 처리: {} → Redis 키 삭제: {}", username, key);
    }

    // 닉네임 중복검사 (회원가입 시)
    public boolean isNicknameTaken(String name) {
        return memberRepository.existsByname(name);
    }

    // 닉네임 중복검사 (수정 시, 본인 제외)
    public boolean isNicknameTaken(String name, String currentUsername) {
        return memberRepository.findByname(name)
                .map(existing -> !existing.getUsername().equals(currentUsername))
                .orElse(false);
    }

    // 닉네임 변경 (username=이메일로 식별)
    // username(email) 기반으로 찾기
    public void updateNickname(String currentUsername, String newName) {
        Member member = memberRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("회원 없음"));

        if (isNicknameTaken(newName, currentUsername)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        member.setName(newName);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member getByUsername(String username) {
        log.debug("getByUsername 호출됨, username={}", username);
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자"));
    }

    // email로 아이디 찾기용
    public Optional<String> findByNickname(String email) {
        return memberRepository.findByUsername(email)
                .map(Member::getName);
    }

    public List<MemberResponseDTO> getAllMembers() {
        List<Member> members = memberRepository.findAll(); // 모든 회원 조회
        return members.stream()
                .map(MemberMapper::toDTO) // DTO로 변환
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDTO> searchMembers(String name, Long myMno) {
        List<Member> found = memberRepository.findAllByName(name);
        return found.stream()
                .filter(m -> !m.getMno().equals(myMno))
                .map(MemberMapper::toDTO)
                .toList();
    }
}
