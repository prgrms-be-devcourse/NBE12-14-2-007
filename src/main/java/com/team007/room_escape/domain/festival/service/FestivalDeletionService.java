package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.service.MemberTrustGradeService;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 삭제와 복구 후 등급 계산을 모든 호출 경로에서 동일하게 처리한다. */
@Service
@RequiredArgsConstructor
public class FestivalDeletionService {

	private final FestivalRepository festivalRepository;
	private final MemberTrustGradeService memberTrustGradeService;
	private final DeletionHistoryService deletionHistoryService;
	private final FestivalDuplicateValidator duplicateValidator;

	@Transactional
	public void delete(Festival festival) {
		festival.delete();
		refreshGrade(festival);
	}

	@Transactional
	public void deleteByAuthor(Festival festival, UUID actorId) {
		delete(festival);
		deletionHistoryService.recordAuthorDeletion(DeletionTargetType.FESTIVAL,
			festival.getId().toString(), actorId);
	}

	@Transactional
	public void restore(Festival festival) {
		validateRestoreDuplicate(festival);
		festival.restore();
		refreshGrade(festival);
	}

	/** 새 중복 정책은 적용하지 않고 기존 회원 제보의 날짜·지역·링크 기준을 사용한다. */
	private void validateRestoreDuplicate(Festival festival) {
		if (festival.getProviderType() != ProviderType.MEMBER) {
			return;
		}
		if (festival.getBeginDe() == null || festival.getEndDe() == null
			|| festival.getRegion() == null || festival.getUrl() == null || festival.getUrl().isBlank()) {
			throw new BusinessException(FestivalExceptionCode.RESTORE_DATA_INCOMPLETE);
		}
		duplicateValidator.validate(festival.getId(),
			festival.getBeginDe(), festival.getEndDe(), festival.getRegion(), festival.getUrl());
	}

	private void refreshGrade(Festival festival) {
		// 집계 쿼리 전에 삭제/복구 상태를 DB에 반영한다.
		festivalRepository.flush();
		memberTrustGradeService.refreshForFestival(festival);
	}
}
