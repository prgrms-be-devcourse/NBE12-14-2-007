package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Builder;

public class FestivalSubmissionRequest {

	private FestivalSubmissionRequest() {
	}

	@Builder
	@Schema(name = "FestivalSubmissionUpsertRequest", description = "행사 제보 등록·수정 요청")
	public record Upsert(
		@Schema(description = "기관명", example = "방구석탈출")
		String instNm,

		@NotBlank(message = "행사 이름은 필수입니다.")
		@Schema(description = "행사 이름", example = "성수 독립 플리마켓")
		String title,

		@NotBlank(message = "제보 행사 카테고리는 필수입니다.")
		@Size(max = 50, message = "제보 행사 카테고리는 50자 이하여야 합니다.")
		@Schema(description = "제보 행사 카테고리", example = "플리마켓")
		String category,

		@Schema(description = "공개할 행사 상세 내용")
		String festivalContent,

		@NotBlank(message = "행사 참고 링크는 필수입니다.")
		@Size(max = 2048, message = "행사 참고 링크는 2,048자 이하여야 합니다.")
		@Schema(description = "행사 정보를 확인할 수 있는 참고 링크", example = "https://www.instagram.com/example-event")
		String referenceUrl,

		@NotNull(message = "행사 지역은 필수입니다.")
		@Schema(description = "행사 지역", example = "GYEONGGI")
		FestivalRegion region,

		@Size(max = 255, message = "행사 상세 주소는 255자 이하여야 합니다.")
		@Schema(description = "행사 상세 주소", example = "팔달구 효원로 1")
		String regionDetail,

		@Size(max = 2048, message = "이미지 URL은 2,048자 이하여야 합니다.")
		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@NotNull(message = "행사 시작 일시는 필수입니다.")
		@Schema(description = "행사 시작 일시", example = "2026-09-20T10:00:00")
		LocalDateTime beginDe,

		@NotNull(message = "행사 종료 일시는 필수입니다.")
		@Schema(description = "행사 종료 일시", example = "2026-09-20T18:00:00")
		LocalDateTime endDe,

		@Schema(description = "행사 시간 정보", example = "10:00~18:00")
		String eventTmInfo,

		@Schema(description = "참가 비용 정보", example = "무료")
		String partcptExpnInfo,

		@Schema(description = "전화번호", example = "010-1234-5678")
		String telnoInfo,

		@Schema(description = "주최기관명", example = "방구석탈출")
		String hostInstNm
	) {

		@AssertTrue(message = "행사 종료 일시는 시작 일시보다 빠를 수 없습니다.")
		public boolean isValidPeriod() {
			return beginDe == null
				|| endDe == null
				|| !endDe.isBefore(beginDe);
		}
	}
}
