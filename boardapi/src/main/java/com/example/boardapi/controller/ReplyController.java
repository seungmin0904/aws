package com.example.boardapi.controller;

import com.example.boardapi.dto.ReplyDTO;
import com.example.boardapi.dto.ReplyModifyDTO;
import com.example.boardapi.dto.ReplyRequestDTO;
import com.example.boardapi.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    // 댓글 등록
    @PostMapping
    public ResponseEntity<Void> register(@RequestBody ReplyRequestDTO dto,
            @AuthenticationPrincipal(expression = "username") String username) {
        dto.setUsername(username);
        replyService.register(dto);
        return ResponseEntity.ok().build();
    }

    // 댓글 조회 (트리 구조)
    @GetMapping
    public ResponseEntity<List<ReplyDTO>> getReplies(@RequestParam Long bno) {
        return ResponseEntity.ok(replyService.getReplies(bno));
    }

    // 댓글 수정
    @PutMapping("/{rno}")
    public ResponseEntity<Void> modify(@PathVariable Long rno,
            @RequestBody ReplyModifyDTO dto,
            @AuthenticationPrincipal(expression = "username") String username) {

        replyService.modify(rno, dto.getText(), username);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{rno}")
    public ResponseEntity<Void> delete(@PathVariable Long rno,
            @AuthenticationPrincipal(expression = "username") String username) {
        replyService.delete(rno, username);
        return ResponseEntity.ok().build();
    }
}
