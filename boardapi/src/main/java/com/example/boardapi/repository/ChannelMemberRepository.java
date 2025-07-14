package com.example.boardapi.repository;

import com.example.boardapi.entity.ChannelMember;
import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, Long> {

    List<ChannelMember> findByRoom(ChatRoom room);

    List<ChannelMember> findByMember(Member member);

    Optional<ChannelMember> findByMemberAndRoom(Member member, ChatRoom room);

    Optional<ChannelMember> findByRoomIdAndMemberMno(Long roomId, Long memberMno);

    boolean existsByRoomIdAndMemberMno(Long roomId, Long memberMno);

    @Transactional
    void deleteByRoomId(Long roomId);
}
