package com.example.boardapi.repository;

import com.example.boardapi.entity.ChatRoom;
import com.example.boardapi.entity.ChatRoomType;
import com.example.boardapi.entity.Server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 채팅방 이름으로 조회
    // Optional을 사용하여 이름이 없을 경우를 처리
    Optional<ChatRoom> findByName(String name);

    List<ChatRoom> findByRoomTypeAndMembersMemberMno(ChatRoomType roomType, Long memberId);

    @Query("""
                SELECT r FROM ChatRoom r
                JOIN ChatRoomMember m1 ON m1.chatRoom = r
                JOIN ChatRoomMember m2 ON m2.chatRoom = r
                WHERE r.roomType = 'DM'
                AND (
                    (m1.member.mno = :id1 AND m2.member.mno = :id2)
                    OR
                    (m1.member.mno = :id2 AND m2.member.mno = :id1)
                )
            """)
    Optional<ChatRoom> findDmRoomBetween(@Param("id1") Long id1, @Param("id2") Long id2);

    List<ChatRoom> findByServer(Server server);

    @Query("""
                SELECT cr FROM ChatRoom cr
                JOIN cr.members crm
                WHERE cr.roomType = 'DM' AND crm.member.id = :memberId
            """)
    List<ChatRoom> findMyDmRooms(@Param("memberId") Long memberId);
}
