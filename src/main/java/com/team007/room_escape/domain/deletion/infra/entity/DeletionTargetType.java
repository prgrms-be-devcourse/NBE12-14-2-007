package com.team007.room_escape.domain.deletion.infra.entity;

import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.DeletionExceptionCode;
import java.util.UUID;

/** 신고 대상과 달리 회원·문의는 콘텐츠 삭제 대상에 포함하지 않는다. */
public enum DeletionTargetType {
	FESTIVAL, POST, COMMENT, COMMUNITY_POST, COMMUNITY_COMMENT;

	public String normalizeId(String id) {
		if (id == null || id.isBlank()) {
			throw new BusinessException(DeletionExceptionCode.INVALID_TARGET_ID);
		}
		try {
			if (this == POST || this == COMMUNITY_POST) {
				String normalized = UUID.fromString(id.trim()).toString();
				if (!normalized.equalsIgnoreCase(id.trim())) {
					throw new IllegalArgumentException();
				}
				return normalized;
			}
			long number = Long.parseLong(id.trim());
			if (number <= 0) {
				throw new IllegalArgumentException();
			}
			return Long.toString(number);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(DeletionExceptionCode.INVALID_TARGET_ID);
		}
	}
}
