package com.team007.room_escape.domain.community.service;

import com.team007.room_escape.domain.community.infra.repository.CommunityPostRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityCommentRepository;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHandler;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommunityExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityCommentDeletionHandler implements DeletionHandler {
	private final CommunityCommentRepository comments;
	private final DeletionHistoryService histories;
	private final CommunityPostRepository parents;

	@Override
	public void restore(DeletionHistory history) {
		var target = comments.findForRestoration(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND));
		if (!target.isDeleted()) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND);
		}
		histories.requireAdministratorDeletion(supports(), history.getTargetId());
		var parent = parents.findForRestoration(target.getPost().getId())
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND));
		if (parent.isDeleted()) {
			throw new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND);
		}
		target.restore();
	}

	@Override
	public DeletionTargetType supports() {
		return DeletionTargetType.COMMUNITY_COMMENT;
	}

	@Override
	public void delete(DeletionHistory history) {
		comments.findForDeletion(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(CommunityExceptionCode.COMMENT_NOT_FOUND)).delete();
	}
}
