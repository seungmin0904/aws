package com.example.boardapi.security.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.boardapi.entity.Board;
import com.example.boardapi.entity.Member;
import com.example.boardapi.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 작성자 권한 검증
    public void checkBoardOwnership(Board board, Member currentUser) {
        if (!board.getMember().getUsername().equals(currentUser.getUsername())) {
            throw new SecurityException("작성자만 수정/삭제할 수 있습니다.");
        }
    }

     // 마이페이지용 기존 비밀번호 확인 후 변경
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 없습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            return false;
        }

        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        return true;
    }

    // 비밀번호 찾기용 이메일로 직접 변경
    public boolean resetPasswordByEmail(String email, String newPassword) {
        Optional<Member> memberOpt = memberRepository.findByUsername(email);
        if (memberOpt.isEmpty()) return false;

        Member member = memberOpt.get();
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        return true;
    }
}
