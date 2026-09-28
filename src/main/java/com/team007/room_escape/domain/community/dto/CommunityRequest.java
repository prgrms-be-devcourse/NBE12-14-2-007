package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.type.CommunityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CommunityRequest {

	public record PostUpsert(
		@NotNull CommunityCategory category,
		@NotBlank @Size(min = 2, max = 100) String title,
		@NotBlank String content
	) {
	}

	public record CommentUpsert(
		@NotBlank @Size(max = 500) String content
	) {
	}
}
