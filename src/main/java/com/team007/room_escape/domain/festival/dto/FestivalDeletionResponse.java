package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalDeletionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

public class FestivalDeletionResponse {

	private FestivalDeletionResponse() {
	}

	@Builder
	@Schema(name = "FestivalDeletionInfo", description = "현재 삭제 상태. 삭제되지 않았으면 null")
	public record Info(
		@Schema(description = "행사 삭제 시각") LocalDateTime deletedAt,
		@Schema(description = "삭제 주체 구분. AUTHOR, ADMIN, UNKNOWN") FestivalDeletionType type,
		@Schema(description = "작성자에게 안내하는 삭제 사유") String reason
	) {
		public static Info from(Festival festival) {
			if (!festival.isDeleted()) {
				return null;
			}
			return Info.builder()
				.deletedAt(festival.getDeletedAt())
				.type(festival.getDeletionType())
				.reason(festival.getDeletionReason())
				.build();
		}
	}
}
