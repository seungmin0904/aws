package com.example.boardapi.service;

import com.example.boardapi.dto.ReplyDTO;
import com.example.boardapi.dto.ReplyRequestDTO;
import com.example.boardapi.entity.Board;
import com.example.boardapi.entity.Member;
import com.example.boardapi.entity.Reply;
import com.example.boardapi.mapper.ReplyMapper;
import com.example.boardapi.repository.BoardRepository;
import com.example.boardapi.repository.MemberRepository;
import com.example.boardapi.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ReplyMapper replyMapper;

    // 댓글 등록
    @Transactional
    public void register(ReplyRequestDTO dto) {
        Board board = boardRepository.findById(dto.getBno())
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        Member member = memberRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Reply parent = (dto.getParentRno() != null)
                ? replyRepository.findById(dto.getParentRno()).orElse(null)
                : null;

        Reply reply = replyMapper.toEntity(dto, board, member, parent);
        replyRepository.save(reply);
    }

    // 댓글 트리 조회
    public List<ReplyDTO> getReplies(Long bno) {
        Board board = boardRepository.findById(bno)
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));

        List<Reply> entities = replyRepository.findByBoardOrderByCreatedDateAsc(board);

        Map<Long, ReplyDTO> dtoMap = new HashMap<>();
        Map<Long, List<ReplyDTO>> childrenBuffer = new HashMap<>();
        List<ReplyDTO> rootReplies = new ArrayList<>();

        // 4. 반복문으로 모든 댓글을 DTO로 변환 + 트리 조립
        for (Reply reply : entities) {
            Long currentRno = reply.getRno();
            Long parentRno = reply.getParent() != null ? reply.getParent().getRno() : null;
            ReplyDTO dto = dtoMap.computeIfAbsent(currentRno, rno -> replyMapper.toDTO(reply));

            if (parentRno != null) {
                if (dtoMap.containsKey(parentRno)) {
                    dtoMap.get(parentRno).getChildren().add(dto);
                } else {
                    childrenBuffer.computeIfAbsent(parentRno, k -> new ArrayList<>()).add(dto);
                }
            }
            if (parentRno == null && !rootReplies.contains(dto)) {
                rootReplies.add(dto);
            }

            // 현재 댓글이 다른 대기 중인 자식들을 갖고 있는 경우 연결
            if (childrenBuffer.containsKey(currentRno)) {
                dto.getChildren().addAll(childrenBuffer.get(currentRno));
                childrenBuffer.remove(currentRno);
            }
        }

        // 5. 로그 확인 (선택)
        log.info("▶ 전체 댓글 수: {}", entities.size());
        log.info("▶ 루트 댓글 수: {}", rootReplies.size());

        return rootReplies;
    }

    // 댓글 수정
    @Transactional
    public void modify(Long rno, String text, String username) {
        Reply reply = replyRepository.findById(rno)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        if (!reply.getMember().getUsername().equals(username)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        reply.updateText(text);
    }

    // 댓글 삭제 (soft delete 방식)
    @Transactional
    public void delete(Long rno, String username) {
        Reply reply = replyRepository.findById(rno)
                .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));

        if (!reply.getMember().getUsername().equals(username)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        reply.updateText("삭제된 댓글입니다.");
    }
}
