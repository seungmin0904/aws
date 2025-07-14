package com.example.boardapi.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyDTO {

    private Long rno;
    private Long bno;
    private Long parentRno;
    private String text;
    private String replyer; // Member.name
    private String username; // Member.username
    private boolean deleted;
    private LocalDateTime createdDate;
    private String parentWriterName;

    @Builder.Default
    private List<ReplyDTO> children = new ArrayList<>();
}
