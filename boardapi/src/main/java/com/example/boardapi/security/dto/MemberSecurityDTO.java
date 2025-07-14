package com.example.boardapi.security.dto;

import com.example.boardapi.entity.Member;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberSecurityDTO implements UserDetails, Principal {
    private Long mno;
    private String username; // 이메일
    private String password;
    private String name;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));

    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getUsername() {

        return username;

    }

    @Override
    public String getName() {
        return username; // 또는 name
    }

    public String getNickname() {
        return name;
    }

    public static MemberSecurityDTO from(Member member) {
        return MemberSecurityDTO.builder()
                .mno(member.getMno())
                .username(member.getUsername())
                .password(member.getPassword())
                .name(member.getName())
                .build();
    }
}
