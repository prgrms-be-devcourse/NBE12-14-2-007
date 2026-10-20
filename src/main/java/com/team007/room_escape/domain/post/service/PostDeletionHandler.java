package com.team007.room_escape.domain.post.service;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHandler;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.domain.post.infra.repository.PostRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.PostExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostDeletionHandler implements DeletionHandler {
	private final PostRepository posts;
	private final DeletionHistoryService histories;

	@Override
	public void restore(DeletionHistory history) {
		var target = posts.findForRestoration(UUID.fromString(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND));
		if (!target.isDeleted()) {
			throw new BusinessException(PostExceptionCode.POST_NOT_FOUND);
		}
		histories.requireAdministratorDeletion(supports(), history.getTargetId());
		target.restore();
	}

	@Override
	public DeletionTargetType supports() {
		return DeletionTargetType.POST;
	}

	@Override
	public void delete(DeletionHistory history) {
		posts.findForDeletion(UUID.fromString(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(PostExceptionCode.POST_NOT_FOUND)).delete();
	}
}
