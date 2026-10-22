package com.team007.room_escape.domain.deletion.service;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import com.team007.room_escape.global.response.code.DeletionExceptionCode;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.domain.deletion.infra.entity.*;
import com.team007.room_escape.domain.deletion.infra.repository.DeletionHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeletionHistoryService {
	private final DeletionHistoryRepository repository;

	/** 대상의 상태 변경과 같은 트랜잭션으로만 기록한다. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(DeletionHistory history) {
		repository.save(history);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void recordAuthorDeletion(DeletionTargetType type, String targetId, UUID authorId) {
		repository.save(DeletionHistory.record(type, targetId, DeletionAction.DELETE,
			authorId, DeletionActorType.AUTHOR, DeletionReason.AUTHOR_REQUEST, null));
	}

	public Optional<DeletionHistory> findLatest(DeletionTargetType type, String targetId) {
		return repository.findFirstByTargetTypeAndTargetIdOrderByIdDesc(type, type.normalizeId(targetId));
	}

	public Map<String, DeletionHistory> findLatestForTargets(DeletionTargetType type, List<String> ids) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		var normalized = ids.stream().map(type::normalizeId).distinct().toList();
		return repository.findLatestForTargets(type, normalized).stream()
			.collect(Collectors.toMap(DeletionHistory::getTargetId, Function.identity()));
	}

	/** 대상 행을 잠근 뒤 호출한다. 마지막 조치가 관리자 삭제인지 확인한다. */
	public void requireAdministratorDeletion(DeletionTargetType type, String targetId) {
		DeletionHistory latest = findLatest(type, targetId)
			.orElseThrow(() -> new BusinessException(DeletionExceptionCode.RESTORE_HISTORY_NOT_FOUND));
		if (latest.getAction() != DeletionAction.DELETE || latest.getActorType() != DeletionActorType.ADMIN) {
			throw new BusinessException(DeletionExceptionCode.RESTORE_NOT_ALLOWED);
		}
	}
}
