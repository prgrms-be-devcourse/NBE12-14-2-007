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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

// CommunityComment엔 @SQLRestriction이 없어서 삭제된 댓글도 조회된다. 살아 있는 댓글만 볼 때는 DeletedAtIsNull 조건을 붙인다.
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
	/** 복구 전용: 삭제 필터를 우회하고 대상 행을 잠근다. */
	@Query(value = "select * from community_comment where id = :id for update", nativeQuery = true)
	Optional<CommunityComment> findForRestoration(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CommunityComment c where c.id = :id and c.deletedAt is null")
	Optional<CommunityComment> findForDeletion(@Param("id") Long id);

	boolean existsByMemberIdAndDeletedAtIsNullAndCreatedAtAfter(UUID memberId, LocalDateTime createdAfter);

	boolean existsByMemberIdAndContentAndDeletedAtIsNullAndCreatedAtAfter(
		UUID memberId,
		String content,
		LocalDateTime createdAfter
	);

	interface PostCommentCount {
		UUID getPostId();
		long getCommentCount();
	}

	/** 글별 댓글 수. 대댓글은 포함하고 삭제된 댓글은 뺀다. */
	@Query("""
		select c.post.id as postId, count(c.id) as commentCount
		from CommunityComment c
		where c.post.id in :postIds
		  and c.deletedAt is null
		group by c.post.id
		""")
	List<PostCommentCount> countByPostIds(@Param("postIds") Collection<UUID> postIds);

	/**
	 * 글의 일반 댓글 한 페이지. 페이지는 일반 댓글 기준으로 나눠 대댓글이 두 페이지로 갈리지 않게 한다.
	 * 삭제된 댓글도 "삭제된 댓글입니다"로 자리를 남기려고 포함한다. 정렬은 오래된 순으로 고정한다.
	 */
	@Query(
		value = """
			select c
			from CommunityComment c
			join fetch c.member
			where c.post.id = :postId
			  and c.parent is null
			order by c.createdAt asc, c.id asc
			""",
		countQuery = """
			select count(c)
			from CommunityComment c
			where c.post.id = :postId
			  and c.parent is null
			"""
	)
	Page<CommunityComment> findRootsByPostId(@Param("postId") UUID postId, Pageable pageable);

	/** 일반 댓글들에 달린 대댓글을 한 번에 가져온다. 삭제된 대댓글도 포함하고, 오래된 순이다. */
	@Query("""
		select c
		from CommunityComment c
		join fetch c.member
		where c.parent.id in :parentIds
		order by c.createdAt asc, c.id asc
		""")
	List<CommunityComment> findRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

	/** 삭제 여부와 상관없이 찾는다. 관리자가 신고된 댓글의 글을 찾을 때 쓴다. */
	@EntityGraph(attributePaths = {"member", "post"})
	Optional<CommunityComment> findById(Long id);

	/** 삭제되지 않은 댓글. 수정·삭제·답글 달기처럼 살아 있는 댓글만 다루는 곳에서 쓴다. */
	@EntityGraph(attributePaths = {"member", "post"})
	Optional<CommunityComment> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	/** 글의 댓글 수. 대댓글은 포함하고 삭제된 댓글은 뺀다. */
	long countByPostIdAndDeletedAtIsNull(UUID postId);
}
