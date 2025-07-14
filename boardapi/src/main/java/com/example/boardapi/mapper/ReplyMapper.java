package com.example.boardapi.mapper;

import com.example.boardapi.dto.ReplyDTO;
import com.example.boardapi.dto.ReplyRequestDTO;
import com.example.boardapi.entity.*;

import org.springframework.stereotype.Component;

@Component
public class ReplyMapper {

    public ReplyDTO toDTO(Reply entity) {
        // 부모 작성자 이름 가져오기
        String parentWriterName = null;
        if (entity.getParent() != null && entity.getParent().getMember() != null) {
            parentWriterName = entity.getParent().getMember().getName();
        }
        return ReplyDTO.builder()
                .rno(entity.getRno())
                .bno(entity.getBoard().getBno())
                .text(entity.isDeleted() ? "삭제된 댓글" : entity.getText())
                .replyer(entity.getMember().getName())
                .username(entity.getMember().getUsername())
                .deleted(entity.isDeleted())
                .createdDate(entity.getCreatedDate())
                .parentRno(entity.getParent() != null ? entity.getParent().getRno() : null)
                .parentWriterName(parentWriterName)
                .build();
    }

    public Reply toEntity(ReplyRequestDTO dto, Board board, Member member, Reply parent) {
        return Reply.builder()
                .text(dto.getText())
                .board(board)
                .member(member)
                .parent(parent)
                .build();
    }
}
