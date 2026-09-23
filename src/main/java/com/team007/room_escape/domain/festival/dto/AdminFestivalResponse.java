package com.team007.room_escape.domain.festival.dto;

import java.time.LocalDateTime;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AdminFestivalResponse {

	private AdminFestivalResponse() {
	}

	/**
	 * 관리자 행사 목록의 한 줄.
	 * 공개 목록(FestivalResponse.ListResponse)과 같은 모양에 deletedAt 만 더했다.
	 * 삭제 시각이 없으면 공개 목록에 그대로 쓸 수 있는데, 그러면 삭제 여부를
	 * 화면에서 알 수 없어 복구 버튼을 어디에 붙일지 판단할 수 없다.
	 */
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

		@Schema(description = "기관명")
		String instNm,

		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

		@Schema(description = "행사 지역")
		FestivalRegion region,

		@Schema(description = "진행 상태. 종료일에서 계산된다", example = "OPEN")
		FestivalStatus status,

		@Schema(description = "삭제 시각. 삭제되지 않았으면 null")
		LocalDateTime deletedAt
	) {

		public static ListItem from(Festival festival) {
			return ListItem.builder()
				.festivalId(festival.getId())
				.providerType(festival.getProviderType())
				.title(festival.getTitle())
				.category(festival.getCategory())
				.instNm(festival.getInstNm())
				.imgUrl(festival.getImgUrl())
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
				.region(festival.getRegion())
				.status(festival.getStatus())
				.deletedAt(festival.getDeletedAt())
				.build();
		}
	}
}
