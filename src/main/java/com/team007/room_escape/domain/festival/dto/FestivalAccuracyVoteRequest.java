package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class FestivalAccuracyVoteRequest {

	private FestivalAccuracyVoteRequest() {
	}

	@Schema(name = "FestivalAccuracyVoteUpsertRequest", description = "행사 정보 정확도 평가 요청")
	public record Upsert(

		@NotNull(message = "평가 종류는 필수입니다.")
		@Schema(description = "행사 정보 정확도 평가", example = "ACCURATE")
		FestivalAccuracyVoteType voteType
	) {
	}
}
