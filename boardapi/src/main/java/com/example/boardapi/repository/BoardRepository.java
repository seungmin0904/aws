package com.example.boardapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.boardapi.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {

    @Query("SELECT b FROM Board b JOIN FETCH b.member WHERE b.bno = :bno")
    Optional<Board> findByIdWithMember(@Param("bno") Long bno);
}
