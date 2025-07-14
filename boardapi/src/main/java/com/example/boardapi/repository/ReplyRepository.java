package com.example.boardapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.boardapi.entity.Board;
import com.example.boardapi.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
     List<Reply> findByBoardOrderByCreatedDateAsc(Board board);

     @Query("SELECT r FROM Reply r JOIN FETCH r.member WHERE r.rno = :rno")
     Optional<Reply> findByIdWithMember(@Param("rno") Long rno);
}
