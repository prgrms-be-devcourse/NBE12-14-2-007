package com.team007.room_escape.domain.festival.infra.entity;

import java.time.LocalDateTime;

public enum FestivalStatus {
	OPEN,
	CLOSED;

	public static FestivalStatus from(LocalDateTime endDe) {
		if (endDe == null) {
			return OPEN;
		}

		return endDe.isBefore(LocalDateTime.now())
				? CLOSED
				: OPEN;
	}
}
