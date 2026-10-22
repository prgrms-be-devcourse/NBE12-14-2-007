package com.team007.room_escape.domain.comment.infra.repository;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	/** 복구 대상 행을 잠근다. */
	@Query(value = "select * from comment where id = :id for update", nativeQuery = true)
	Optional<Comment> findForRestoration(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Comment c where c.id = :id and c.deletedAt is null")
	Optional<Comment> findForDeletion(@Param("id") Long id);

	/**
	 * 후기의 일반 댓글 한 페이지. 페이지는 일반 댓글 기준으로 나눠 대댓글이 두 페이지로 갈리지 않게 한다.
	 * 삭제된 댓글도 "삭제된 댓글입니다"로 자리를 남기려고 포함한다. 정렬은 오래된 순으로 고정한다.
	 */
	@Query(
		value = """
			SELECT c
			FROM Comment c
			JOIN FETCH c.member
			WHERE c.post.id = :postId
			  AND c.parent IS NULL
			ORDER BY c.createdAt ASC, c.id ASC
			""",
		countQuery = """
			SELECT COUNT(c)
			FROM Comment c
			WHERE c.post.id = :postId
			  AND c.parent IS NULL
			"""
	)
	Page<Comment> findRootsByPostId(@Param("postId") UUID postId, Pageable pageable);

	/** 일반 댓글들에 달린 대댓글을 한 번에 가져온다. 삭제된 대댓글도 포함하고, 오래된 순이다. */
	@Query("""
		SELECT c
		FROM Comment c
		JOIN FETCH c.member
		WHERE c.parent.id IN :parentIds
		ORDER BY c.createdAt ASC, c.id ASC
		""")
	List<Comment> findRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

	/** 삭제되지 않은 댓글. 수정·삭제·답글 달기처럼 살아 있는 댓글만 다루는 곳에서 쓴다. */
	@EntityGraph(attributePaths = {"member", "post"})
	Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);
}
