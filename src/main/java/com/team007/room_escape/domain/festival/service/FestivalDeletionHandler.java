package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHandler;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FestivalDeletionHandler implements DeletionHandler {
	private final FestivalRepository festivals;
	private final FestivalDeletionService deletion;
	private final DeletionHistoryService histories;

	@Override
	public void restore(DeletionHistory history) {
		var festival = festivals.findForDeletion(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		if (!festival.isDeleted()) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_DELETED);
		}
		histories.requireAdministratorDeletion(supports(), history.getTargetId());
		deletion.restore(festival);
	}

	@Override
	public DeletionTargetType supports() {
		return DeletionTargetType.FESTIVAL;
	}

	@Override
	public void delete(DeletionHistory history) {
		var festival = festivals.findForDeletion(Long.valueOf(history.getTargetId()))
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		deletion.delete(festival);
	}
}
