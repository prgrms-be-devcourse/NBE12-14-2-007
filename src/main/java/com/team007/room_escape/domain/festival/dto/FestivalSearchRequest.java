package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "행사 검색 조건")
public record FestivalSearchRequest(

		@Schema(description = "검색어", example = "수원")
		String keyword,

		@Schema(description = "행사 지역", example = "GYEONGGI_SUWON")
		FestivalRegion region,

		@Schema(description = "데이터 출처", example = "PUBLIC")
		ProviderType providerType,

		@Schema(description = "행사 카테고리", example = "축제")
		String category,

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Schema(description = "행사 진행 일자", example = "2026-09-21")
		LocalDate date,

		@Schema(description = "종료 행사 제외 여부", example = "false", defaultValue = "false")
		Boolean excludeClosed
) {
}
