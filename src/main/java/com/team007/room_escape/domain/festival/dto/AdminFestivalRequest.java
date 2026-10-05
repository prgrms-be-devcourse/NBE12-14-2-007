package com.team007.room_escape.domain.festival.dto;

import java.time.LocalDateTime;
import java.util.Locale;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminFestivalRequest {

	private AdminFestivalRequest() {
	}

	/** 관리자 행사 검색 조건. 모든 항목이 선택이며, 비우면 전체를 조회한다. */
	@Schema(name = "AdminFestivalSearchRequest", description = "관리자 행사 검색 조건")
	public record Search(

		@Schema(description = "행사명·기관명·상세주소 검색어. 부분 일치, 대소문자 무시", example = "수원")
		String keyword,

		@Schema(description = "데이터 출처. PUBLIC(공공) 또는 MEMBER(회원 제보)", example = "PUBLIC")
		ProviderType providerType,

		@Schema(description = "종료된 행사 제외 여부. 기본값 false", defaultValue = "false")
		Boolean excludeClosed,

		@Schema(description = "삭제된 행사 포함 여부. 기본값 false", defaultValue = "false")
		Boolean includeDeleted
	) {

		/** 검색어가 없으면 null 대신 빈 문자열을 돌려준다. */
		public String keywordOrEmpty() {
			if (keyword == null || keyword.isBlank()) {
				return "";
			}

			return keyword.trim().toLowerCase(Locale.ROOT);
		}

		public boolean excludeClosedOrFalse() {
			return Boolean.TRUE.equals(excludeClosed);
		}

		public boolean includeDeletedOrFalse() {
			return Boolean.TRUE.equals(includeDeleted);
		}
	}

	/** 관리자 행사 수정 요청. 보낸 값으로 전부 덮어쓰며, 출처와 작성자는 바꿀 수 없다. */
	@Schema(name = "AdminFestivalUpdateRequest", description = "관리자 행사 수정 요청")
	public record Update(

		@Size(max = 255)
		@Schema(description = "기관명", example = "경기문화재단")
		String instNm,

		@NotBlank(message = "행사 이름은 필수입니다.")
		@Size(max = 255, message = "행사 이름은 255자 이하여야 합니다.")
		@Schema(description = "행사 이름", example = "수원 화성 문화제")
		String title,

		@NotBlank(message = "행사 카테고리는 필수입니다.")
		@Size(max = 50, message = "행사 카테고리는 50자 이하여야 합니다.")
		@Schema(description = "행사 카테고리", example = "축제")
		String category,

		@Schema(description = "행사 소개", example = "화성행궁 일대에서 열리는 가을 축제입니다.")
		String festivalContent,

		@Size(max = 2048, message = "행사 참고 링크는 2,048자 이하여야 합니다.")
		@Schema(description = "행사 참고 링크", example = "https://example.com/festival")
		String referenceUrl,

		@NotNull(message = "행사 지역은 필수입니다.")
		@Schema(description = "행사 지역", example = "GYEONGGI_SUWON")
		FestivalRegion region,

		@Size(max = 255, message = "행사 상세 주소는 255자 이하여야 합니다.")
		@Schema(description = "행사 상세 주소", example = "수원시 팔달구 정조로 825")
		String regionDetail,

		@Size(max = 2048, message = "이미지 URL은 2,048자 이하여야 합니다.")
		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@NotNull(message = "행사 시작 일시는 필수입니다.")
		@Schema(description = "행사 시작 일시", example = "2026-10-01T10:00:00")
		LocalDateTime beginDe,

		@NotNull(message = "행사 종료 일시는 필수입니다.")
		@Schema(description = "행사 종료 일시", example = "2026-10-05T18:00:00")
		LocalDateTime endDe,

		@Size(max = 255)
		@Schema(description = "행사 시간 정보", example = "10:00~18:00")
		String eventTmInfo,

		@Size(max = 255)
		@Schema(description = "참가 비용 정보", example = "무료")
		String partcptExpnInfo,

		@Size(max = 255)
		@Schema(description = "전화번호", example = "031-123-4567")
		String telnoInfo,

		@Size(max = 255)
		@Schema(description = "주최 기관명", example = "수원시")
		String hostInstNm
	) {

		/** 종료일이 시작일보다 앞서면 진행 상태 계산이 틀어진다. */
		public boolean hasValidPeriod() {
			return !endDe.isBefore(beginDe);
		}
	}
}
