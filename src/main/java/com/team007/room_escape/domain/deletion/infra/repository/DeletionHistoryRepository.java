package com.team007.room_escape.domain.deletion.infra.repository;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** 이력은 추가와 조회만 제공한다. 현재 상태 확인과 대상 잠금은 호출 측의 책임이다. */
public interface DeletionHistoryRepository extends Repository<DeletionHistory, Long> {
	/** 목록 한 페이지의 대상별 최신 조치를 한 번에 조회한다. */
	@org.springframework.data.jpa.repository.Query("""
		select h from DeletionHistory h where h.id in (
			select max(latest.id) from DeletionHistory latest
			where latest.targetType = :type and latest.targetId in :ids
			group by latest.targetId
		)
		""")
	List<DeletionHistory> findLatestForTargets(
		@org.springframework.data.repository.query.Param("type") DeletionTargetType type,
		@org.springframework.data.repository.query.Param("ids") List<String> ids);

	DeletionHistory save(DeletionHistory history);

	Optional<DeletionHistory> findFirstByTargetTypeAndTargetIdOrderByIdDesc(
		DeletionTargetType targetType, String targetId);

	List<DeletionHistory> findAllByTargetTypeAndTargetIdOrderByIdDesc(
		DeletionTargetType targetType, String targetId);
}
