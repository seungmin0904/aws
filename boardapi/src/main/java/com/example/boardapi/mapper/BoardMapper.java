package com.example.boardapi.mapper;

import org.springframework.stereotype.Component;

import com.example.boardapi.dto.BoardRequestDTO;
import com.example.boardapi.dto.BoardResponseDTO;
import com.example.boardapi.entity.Board;
import com.example.boardapi.entity.Member;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BoardMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Board toEntity(BoardRequestDTO boardRequestDTO, Member member) {
        String attachmentsJson = null;

        try {
            attachmentsJson = boardRequestDTO.getAttachments() != null
                    ? objectMapper.writeValueAsString(boardRequestDTO.getAttachments())
                    : null;
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // 또는 Logger로 처리해도 됨
        }

        String content = boardRequestDTO.getContent();

        return Board.builder()
                .title(boardRequestDTO.getTitle())
                .content(content)
                .attachmentsJson(attachmentsJson) // 첨부파일 목록 (JSON 저장)
                .member(member)
                .build();
    }

    public static BoardResponseDTO toDTO(Board entity) {
        Member member = entity.getMember();

        String writerName = (member != null && member.getMno() != null)
                ? member.getName()
                : "알 수 없음";

        return BoardResponseDTO.builder()
                .bno(entity.getBno())
                .title(entity.getTitle())
                .content(entity.getContent())
                .writerName(writerName)
                .createdDate(entity.getCreatedDate().toString())
                .modifiedDate(entity.getUpdatedDate().toString())
                .build();
    }
}
