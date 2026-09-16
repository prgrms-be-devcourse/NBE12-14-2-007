package com.team007.room_escape.domain.comment.infra.dto;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommentResponse {
    public record CommentCreateResponse(
            @Schema(description = "댓글 ID", example = "1")
            Long id,
            @Schema(description = "후기 ID")
            UUID postId,
            @Schema(description = "작성자 ID")
            UUID memberId,
            @Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
            String content,
            @Schema(description = "댓글 작성 일시", example = "2026-09-16T12:30:00")
            LocalDateTime createdAt
    ) {
        public static CommentCreateResponse from(Comment comment) {
            return new CommentCreateResponse(
                    comment.getId(),
                    comment.getPost().getId(),
                    comment.getMember().getId(),
                    comment.getContent(),
                    comment.getCreatedAt()
            );
        }

    }
}
