package com.example.boardapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.boardapi.dto.PageRequestDTO;
import com.example.boardapi.entity.Board;

public interface BoardRepositoryCustom {
    Page<Board> search(PageRequestDTO requestDTO, Pageable pageable);
}
