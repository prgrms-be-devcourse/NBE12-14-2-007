package com.team007.room_escape.domain.comment.infra.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
            @Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
            @NotBlank
            @Size(max = 500)
            String content
) {}

