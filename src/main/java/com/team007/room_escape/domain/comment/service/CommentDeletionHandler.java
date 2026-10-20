package com.team007.room_escape.domain.comment.service;

import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHandler;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommentExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentDeletionHandler implements DeletionHandler {
	private final CommentRepository comments;
	private final DeletionHistoryService histories;
	private final PostRepository parents;

	@Override
	public void restore(DeletionHistory history) {
		var target = comments.findForRestoration(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));
		if (!target.isDeleted()) {
			throw new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND);
		}
		histories.requireAdministratorDeletion(supports(), history.getTargetId());
		var parent = parents.findForRestoration(target.getPost().getId())
			.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND));
		if (parent.isDeleted()) {
			throw new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND);
		}
		target.restore();
	}

	@Override
	public DeletionTargetType supports() {
		return DeletionTargetType.COMMENT;
	}

	@Override
	public void delete(DeletionHistory history) {
		comments.findForDeletion(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(CommentExceptionCode.COMMENT_NOT_FOUND)).delete();
	}
}
