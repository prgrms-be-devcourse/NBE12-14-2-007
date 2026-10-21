package com.team007.room_escape.domain.member.service;

import java.util.Map;
import java.util.HashMap;
import com.team007.room_escape.domain.member.dto.MyContentResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team007.room_escape.domain.member.infra.repository.MyContentRepository;
import com.team007.room_escape.domain.deletion.service.DeletionHistoryService;
import com.team007.room_escape.domain.deletion.dto.DeletionHistoryResponse;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyContentService {
	private final MyContentRepository contents;
	private final MemberReader members;
	private final DeletionHistoryService histories;
	public Page<MyContentResponse.Item> findMine(UUID memberId, boolean comments, Pageable pageable) {
		members.getActiveMember(memberId);
		Page<MyContentRepository.Row> rows = contents.findMine(memberId, comments, pageable);
		var latest = new HashMap<DeletionTargetType, Map<String, DeletionHistory>>();
		for (DeletionTargetType type : DeletionTargetType.values()) {
			latest.put(type, histories.findLatestForTargets(type, rows.getContent().stream()
				.filter(row -> row.type() == type && row.deletedAt() != null).map(MyContentRepository.Row::targetId).toList()));
		}
		return rows.map(row -> {
			DeletionHistory history = latest.get(row.type()).get(row.targetId());
			return new MyContentResponse.Item(row.type(), row.targetId(), row.title(), row.createdAt(), row.deletedAt(),
				row.parentDeleted(), row.deletedAt() != null || row.parentDeleted() ? null : row.originalPath(),
				history == null ? null : DeletionHistoryResponse.Latest.from(history));
		});
	}
}
