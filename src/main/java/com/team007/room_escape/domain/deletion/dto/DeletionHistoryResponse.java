package com.team007.room_escape.domain.deletion.dto;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionAction;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionActorType;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionReason;
import java.time.LocalDateTime;

public class DeletionHistoryResponse {
	private DeletionHistoryResponse() {
	}

	/** 최근 조치 정보다. 현재 삭제 여부와 복구 가능 여부 자체를 의미하지 않는다. */
	public record Latest(
		DeletionAction action,
		DeletionActorType actorType,
		DeletionReason reasonCode,
		String reasonLabel,
		String reasonDetail,
		LocalDateTime createdAt
	) {
		public static Latest from(DeletionHistory history) {
			return new Latest(history.getAction(), history.getActorType(), history.getReason(),
				history.getReason().getDescription(), history.getReasonDetail(), history.getCreatedAt());
		}
	}
}
