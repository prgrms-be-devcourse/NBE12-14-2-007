package com.team007.room_escape.domain.deletion.dto;

import com.team007.room_escape.domain.deletion.infra.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

public class AdminDeletionRequest {
	private AdminDeletionRequest() {
	}

	@Schema(name = "AdminDeletionCreate", description = "관리자 콘텐츠 삭제 조치")
	public record Create(
		@NotNull DeletionTargetType targetType,
		@NotBlank @Size(max = 64) String targetId,
		@NotNull DeletionReason reasonCode,
		@Size(max = 500) String reasonDetail,
		UUID inquiryId
	) {
	}

	@Schema(name = "AdminDeletionRestore", description = "관리자가 삭제한 콘텐츠 복구 조치")
	public record Restore(
		@NotNull DeletionTargetType targetType,
		@NotBlank @Size(max = 64) String targetId
	) {
	}
}
