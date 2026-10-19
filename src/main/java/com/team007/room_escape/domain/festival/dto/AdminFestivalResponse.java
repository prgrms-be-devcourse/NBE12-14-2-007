package com.team007.room_escape.domain.festival.dto;

import java.time.LocalDateTime;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionAction;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AdminFestivalResponse {

	private AdminFestivalResponse() {
	}

	/** 관리자 행사 목록 항목. 삭제 정보로 안내 사유와 복구 가능 여부를 판단한다. */
	@Builder
	@Schema(name = "AdminFestivalListItem", description = "관리자 행사 목록 항목")
	public record ListItem(

		@Schema(description = "행사 번호")
		Long festivalId,

		@Schema(description = "데이터 출처", example = "PUBLIC")
		ProviderType providerType,

		@Schema(description = "행사 제목")
		String title,

		@Schema(description = "행사 종류")
		String category,

		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

		@Schema(description = "행사 등록 일시")
		LocalDateTime createdAt,

		@Schema(description = "행사 지역")
		FestivalRegion region,

		@Schema(description = "진행 상태. 종료일에서 계산된다", example = "OPEN")
		FestivalStatus status,

		@Schema(description = "삭제 시각. 삭제되지 않았으면 null")
		LocalDateTime deletedAt,

		@Schema(description = "삭제 주체 구분 및 안내 사유")
		FestivalResponse.DeletionInfo deletion
	) {

		public static ListItem from(Festival festival, DeletionHistory latest) {
			return ListItem.builder()
				.festivalId(festival.getId())
				.providerType(festival.getProviderType())
				.title(festival.getTitle())
				.category(festival.getCategory())
				.imgUrl(festival.getImgUrl())
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
				.createdAt(festival.getCreatedAt())
				.region(festival.getRegion())
				.status(festival.getStatus())
				.deletedAt(festival.getDeletedAt())
				.deletion(deletionFromHistory(festival, latest))
				.build();
		}

		private static FestivalResponse.DeletionInfo deletionFromHistory(Festival festival, DeletionHistory latest) {
			if (!festival.isDeleted() || latest == null || latest.getAction() != DeletionAction.DELETE) {
				return null;
			}
			return FestivalResponse.DeletionInfo.from(festival, latest);
		}
	}
}
