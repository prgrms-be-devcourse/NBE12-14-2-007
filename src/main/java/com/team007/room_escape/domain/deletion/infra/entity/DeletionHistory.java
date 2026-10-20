package com.team007.room_escape.domain.deletion.infra.entity;

import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.DeletionExceptionCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 발생한 조치의 기록이다. 현재 삭제 여부는 각 콘텐츠의 deletedAt이 기준이다. */
@Entity
@Table(name = "deletion_history")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletionHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 32, updatable = false)
	private DeletionTargetType targetType;

	@Column(name = "target_id", nullable = false, length = 64, updatable = false)
	private String targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16, updatable = false)
	private DeletionAction action;

	@Column(name = "actor_id", nullable = false, updatable = false)
	private UUID actorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false, length = 16, updatable = false)
	private DeletionActorType actorType;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", nullable = false, length = 32, updatable = false)
	private DeletionReason reason;

	@Column(name = "reason_detail", length = 500, updatable = false)
	private String reasonDetail;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public static DeletionHistory record(DeletionTargetType targetType, String targetId,
		DeletionAction action, UUID actorId, DeletionActorType actorType,
		DeletionReason reason, String detail) {
		String text = detail == null || detail.isBlank() ? null : detail.strip();
		if (targetType == null || action == null || actorId == null || actorType == null || reason == null
			|| (text != null && text.length() > 500)) {
			throw new BusinessException(DeletionExceptionCode.INVALID_DELETION_REQUEST);
		}
		// 사유가 '기타'면 무엇이 문제였는지 상세로 남겨야 이력만 보고 판단할 수 있다.
		if (reason == DeletionReason.OTHER && text == null) {
			throw new BusinessException(DeletionExceptionCode.REASON_DETAIL_REQUIRED);
		}
		boolean valid = action == DeletionAction.RESTORE
			? actorType == DeletionActorType.ADMIN && reason == DeletionReason.ADMIN_RESTORE
			: actorType == DeletionActorType.AUTHOR
				? reason == DeletionReason.AUTHOR_REQUEST && text == null
				: reason != DeletionReason.AUTHOR_REQUEST && reason != DeletionReason.ADMIN_RESTORE;
		if (!valid) {
			throw new BusinessException(DeletionExceptionCode.INVALID_ACTOR_FOR_ACTION);
		}
		DeletionHistory history = new DeletionHistory();
		history.targetType = targetType;
		history.targetId = targetType.normalizeId(targetId);
		history.action = action;
		history.actorId = actorId;
		history.actorType = actorType;
		history.reason = reason;
		history.reasonDetail = text;
		history.createdAt = LocalDateTime.now();
		return history;
	}
}
