package com.team007.room_escape.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {

	private CommentRequest() {
	}

	/** 댓글 작성 요청. 수정 때 부모가 바뀌면 안 되므로 parentId는 작성 요청에만 있다. */
	@Schema(name = "CommentCreateRequest", description = "댓글 작성 요청")
	public record Create(
		@Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
		@NotBlank
		@Size(max = 500)
		String content,

		@Schema(description = "답글을 달 댓글 ID. 일반 댓글이면 비운다. 대댓글을 지정하면 그 대댓글의 부모 아래에 붙는다.",
			example = "1", nullable = true)
		Long parentId
	) {
	}

	@Schema(name = "CommentUpsertRequest", description = "댓글 수정 요청")
	public record Upsert(
		@Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
		@NotBlank
		@Size(max = 500)
		String content
	) {
	}
}
