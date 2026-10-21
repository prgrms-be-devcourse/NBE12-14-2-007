package com.team007.room_escape.domain.member.infra.repository;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 본인 이력 전용 조회. 일반 공개 조회의 삭제 필터를 변경하지 않는다. */
@Repository
@RequiredArgsConstructor
public class MyContentRepository {
	private final EntityManager entityManager;
	private static final String POSTS = """
		select 'FESTIVAL' as type, cast(f.id as text) as target_id, f.title, s.created_at, f.deleted_at,
		false as parent_deleted, '/events/' || f.id as original_path
		from festival_submission s join festival f on f.id = s.festival_id where f.member_id = :memberId and f.provider_type = 'MEMBER'
		union all
		select 'POST', cast(p.id as text), p.title, p.created_at, p.deleted_at, false, '/reviews/' || p.id
		from post p where p.member_id = :memberId
		union all
		select 'COMMUNITY_POST', cast(p.id as text), p.title, p.created_at, p.deleted_at, false, '/community/' || p.id
		from community_post p where p.member_id = :memberId
		""";
	private static final String COMMENTS = """
		select 'COMMENT' as type, cast(c.id as text) as target_id, c.content as title, c.created_at, c.deleted_at,
		(p.deleted_at is not null) as parent_deleted, '/reviews/' || p.id as original_path
		from comment c join post p on p.id = c.post_id where c.member_id = :memberId
		union all
		select 'COMMUNITY_COMMENT', cast(c.id as text), c.content, c.created_at, c.deleted_at,
		(p.deleted_at is not null), '/community/' || p.id
		from community_comment c join community_post p on p.id = c.post_id where c.member_id = :memberId
		""";

	public record Row(DeletionTargetType type, String targetId, String title, LocalDateTime createdAt,
		LocalDateTime deletedAt, boolean parentDeleted, String originalPath) { }

	@SuppressWarnings("unchecked")
	public Page<Row> findMine(UUID memberId, boolean comments, Pageable pageable) {
		String source = comments ? COMMENTS : POSTS;
		long total = ((Number) entityManager.createNativeQuery("select count(*) from (" + source + ") mine")
			.setParameter("memberId", memberId).getSingleResult()).longValue();
		List<Object[]> results = entityManager.createNativeQuery("select * from (" + source
			+ ") mine order by created_at desc, type, target_id")
			.setParameter("memberId", memberId).setFirstResult(Math.toIntExact(pageable.getOffset()))
			.setMaxResults(pageable.getPageSize()).getResultList();
		return new PageImpl<>(results.stream().map(row -> new Row(DeletionTargetType.valueOf((String) row[0]),
			(String) row[1], (String) row[2], time(row[3]), time(row[4]), (Boolean) row[5], (String) row[6])).toList(), pageable, total);
	}

	private static LocalDateTime time(Object value) {
		if (value == null) return null;
		return value instanceof LocalDateTime time ? time : ((Timestamp) value).toLocalDateTime();
	}
}
