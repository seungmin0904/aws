package com.example.boardapi.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.boardapi.dto.PageRequestDTO;
import com.example.boardapi.entity.Board;
import com.example.boardapi.entity.QBoard;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

public class BoardRepositoryImpl implements BoardRepositoryCustom{
    
    private final JPAQueryFactory queryFactory;
    private final QBoard board = QBoard.board;

    public BoardRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

     @Override
    public Page<Board> search(PageRequestDTO requestDTO, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        String type = requestDTO.getType();
        String keyword = requestDTO.getKeyword();

        if (type != null && keyword != null) {
            if (type.contains("t")) {
                builder.or(board.title.containsIgnoreCase(keyword));
            }
            if (type.contains("c")) {
                builder.or(board.content.containsIgnoreCase(keyword));
            }
             if (type.contains("w")) {
                builder.or(board.member.name.containsIgnoreCase(keyword));  
            }
        }

        List<Board> content = queryFactory
                .selectFrom(board)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(board.createdDate.desc())
                .fetch();

        long count = queryFactory
                .select(board.count())
                .from(board)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, count);
    }
}
