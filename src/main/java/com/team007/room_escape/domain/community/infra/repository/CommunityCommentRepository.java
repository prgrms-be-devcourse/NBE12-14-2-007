package com.team007.room_escape.domain.community.infra.repository;

import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
	/** 복구 전용: 삭제 필터를 우회하고 대상 행을 잠근다. */
	@Query(value = "select * from community_comment where id = :id for update", nativeQuery = true)
	java.util.Optional<CommunityComment> findForRestoration(@org.springframework.data.repository.query.Param("id") Long id);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CommunityComment c where c.id = :id and c.deletedAt is null")
	Optional<CommunityComment> findForDeletion(@Param("id") Long id);

	boolean existsByMemberIdAndCreatedAtAfter(UUID memberId, LocalDateTime createdAfter);

	boolean existsByMemberIdAndContentAndCreatedAtAfter(
		UUID memberId,
		String content,
		LocalDateTime createdAfter
	);

	interface PostCommentCount {
		UUID getPostId();
		long getCommentCount();
	}

	@Query("""
		select c.post.id as postId, count(c.id) as commentCount
		from CommunityComment c
		where c.post.id in :postIds
		group by c.post.id
		""")
	List<PostCommentCount> countByPostIds(@Param("postIds") Collection<UUID> postIds);

	@EntityGraph(attributePaths = "member")
	Page<CommunityComment> findAllByPostId(UUID postId, Pageable pageable);

	@EntityGraph(attributePaths = {"member", "post"})
	Optional<CommunityComment> findById(Long id);

	long countByPostId(UUID postId);
}
