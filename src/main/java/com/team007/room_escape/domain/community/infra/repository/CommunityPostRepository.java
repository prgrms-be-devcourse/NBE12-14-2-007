package com.team007.room_escape.domain.community.infra.repository;

import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, UUID> {

	@Modifying
	@Query("""
		update CommunityPost p
		set p.viewCount = p.viewCount + 1
		where p.id = :postId
		  and p.deletedAt is null
		""")
	int increaseViewCount(@Param("postId") UUID postId);

	boolean existsByMemberIdAndCreatedAtAfter(UUID memberId, LocalDateTime createdAfter);

	boolean existsByMemberIdAndContentAndCreatedAtAfter(
		UUID memberId,
		String content,
		LocalDateTime createdAfter
	);

	@EntityGraph(attributePaths = "member")
	@Query("""
		select p from CommunityPost p
		where (:category is null or p.category = :category)
		  and (:keyword = ''
		       or lower(p.title) like lower(concat('%', :keyword, '%'))
		       or lower(p.content) like lower(concat('%', :keyword, '%')))
		""")
	Page<CommunityPost> search(
		@Param("category") CommunityCategory category,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "member")
	Optional<CommunityPost> findById(UUID id);
}
