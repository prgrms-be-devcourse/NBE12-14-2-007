package com.team007.room_escape.domain.deletion.service;

import org.springframework.security.access.AccessDeniedException;
import com.team007.room_escape.domain.inquiry.service.InquiryDeletionProcessor;
import com.team007.room_escape.domain.deletion.dto.DeletionHistoryResponse;
import com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest;
import com.team007.room_escape.domain.deletion.infra.entity.*;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.DeletionExceptionCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDeletionFacade {
	private final Map<DeletionTargetType, DeletionHandler> handlers;
	private final DeletionHistoryService histories;
	private final MemberReader members;
	private final InquiryDeletionProcessor inquiries;

	public AdminDeletionFacade(List<DeletionHandler> handlers, DeletionHistoryService histories, MemberReader members,
		InquiryDeletionProcessor inquiries) {
		this.handlers = handlers.stream().collect(Collectors.toMap(DeletionHandler::supports, Function.identity()));
		this.histories = histories;
		this.members = members;
		this.inquiries = inquiries;
	}

	@Transactional
	public void restore(UUID adminId, DeletionTargetType type, String targetId) {
		requireAdministrator(adminId, "관리자만 복구할 수 있습니다.");
		DeletionHistory history = DeletionHistory.record(type, targetId, DeletionAction.RESTORE,
			adminId, DeletionActorType.ADMIN, DeletionReason.ADMIN_RESTORE, null);
		DeletionHandler handler = handlers.get(type);
		if (handler == null) {
			throw new BusinessException(DeletionExceptionCode.HANDLER_NOT_REGISTERED);
		}
		handler.restore(history);
		histories.record(history);
	}

	@Transactional(readOnly = true)
	public DeletionHistoryResponse.Latest latest(
		UUID adminId, DeletionTargetType type, String targetId) {
		requireAdministrator(adminId, "관리자만 조치 이력을 조회할 수 있습니다.");
		if (type == null) {
			throw new BusinessException(DeletionExceptionCode.INVALID_TARGET_TYPE);
		}
		return histories.findLatest(type, targetId)
			.map(DeletionHistoryResponse.Latest::from).orElse(null);
	}

	@Transactional
	public void delete(UUID adminId, AdminDeletionRequest.Create request) {
		requireAdministrator(adminId, "관리자만 삭제할 수 있습니다.");
		DeletionHistory history = DeletionHistory.record(request.targetType(), request.targetId(),
			DeletionAction.DELETE, adminId, DeletionActorType.ADMIN, request.reasonCode(), request.reasonDetail());
		DeletionHandler handler = handlers.get(history.getTargetType());
		if (handler == null) {
			throw new BusinessException(DeletionExceptionCode.HANDLER_NOT_REGISTERED);
		}
		handler.delete(history);
		histories.record(history);
		if (request.inquiryId() != null) {
			inquiries.complete(request.inquiryId(), history);
		}
	}

	/** 기존 토큰의 권한이 아니라 DB의 현재 회원 상태로 판단한다. */
	private void requireAdministrator(UUID adminId, String message) {
		if (members.getActiveMember(adminId).getRole() != MemberRole.ROLE_ADMIN) {
			throw new AccessDeniedException(message);
		}
	}
}
