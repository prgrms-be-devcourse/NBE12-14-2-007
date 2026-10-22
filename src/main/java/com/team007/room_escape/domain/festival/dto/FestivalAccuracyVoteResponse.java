package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public final class FestivalAccuracyVoteResponse {

	private FestivalAccuracyVoteResponse() {
	}

	/** 정확도 평가 결과. 평가와 평가 취소에서 함께 쓴다. */
	@Builder
	@Schema(name = "FestivalAccuracyVoteInfo", description = "행사 정보 정확도 평가 결과")
	public record Info(
		@Schema(description = "정확해요 개수", example = "18")
		long accurateCount,

		@Schema(description = "정보가 달라요 개수", example = "2")
		long inaccurateCount,

		@Schema(description = "현재 사용자의 평가, 평가하지 않았다면 null", example = "ACCURATE", nullable = true)
		FestivalAccuracyVoteType myVote
	) {

		public static Info from(long accurateCount, long inaccurateCount, FestivalAccuracyVoteType myVote) {
			return Info.builder()
				.accurateCount(accurateCount)
				.inaccurateCount(inaccurateCount)
				.myVote(myVote)
				.build();
		}
	}
}
