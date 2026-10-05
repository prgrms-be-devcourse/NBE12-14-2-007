package com.team007.room_escape.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {

	private CommentRequest() {
	}

	@Schema(name = "CommentUpsertRequest", description = "댓글 작성·수정 요청")
	public record Upsert(
		@Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
		@NotBlank
		@Size(max = 500)
		String content
	) {
	}
}
