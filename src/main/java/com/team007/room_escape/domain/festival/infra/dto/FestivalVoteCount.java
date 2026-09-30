package com.team007.room_escape.domain.festival.infra.dto;

import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;

/** 행사별 정확도 투표 수. 목록 조회 때 한 번에 세기 위한 결과다. */
public record FestivalVoteCount(
	Long festivalId,
	FestivalAccuracyVoteType voteType,
	Long count
) {
}
