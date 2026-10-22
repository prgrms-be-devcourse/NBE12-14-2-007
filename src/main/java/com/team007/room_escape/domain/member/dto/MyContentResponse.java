package com.team007.room_escape.domain.member.dto;

import com.team007.room_escape.domain.deletion.dto.DeletionHistoryResponse;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import java.time.LocalDateTime;

public class MyContentResponse {
	private MyContentResponse() {
	}

	public record Item(
		DeletionTargetType type,
		String targetId,
		String title,
		LocalDateTime createdAt,
		LocalDateTime deletedAt,
		boolean parentDeleted,
		String originalPath,
		DeletionHistoryResponse.Latest deletion
	) {
	}
}
