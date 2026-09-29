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
