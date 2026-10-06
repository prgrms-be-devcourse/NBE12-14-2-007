package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.type.CommunityCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CommunityRequest {

	private CommunityRequest() {
	}

	@Schema(name = "CommunityPostUpsertRequest", description = "커뮤니티 글 작성·수정 요청")
	public record PostUpsert(
		@NotNull CommunityCategory category,
		@NotBlank @Size(min = 2, max = 100) String title,
		@NotBlank String content
	) {
	}

	@Schema(name = "CommunityCommentUpsertRequest", description = "커뮤니티 댓글 작성·수정 요청")
	public record CommentUpsert(
		@NotBlank @Size(max = 500) String content
	) {
	}
}
