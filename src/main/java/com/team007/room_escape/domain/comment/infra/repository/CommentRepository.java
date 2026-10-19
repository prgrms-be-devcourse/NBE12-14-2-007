package com.team007.room_escape.domain.comment.infra.repository;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	/** 관리자 조회만 삭제 필터를 우회한다. 공개 조회의 SQLRestriction은 유지한다. */
	@Query(value = "select * from comment where post_id = :postId order by created_at desc, id desc limit :limit offset :offset", nativeQuery = true)
	List<Comment> findAllForAdmin(@org.springframework.data.repository.query.Param("postId") UUID postId,
		@org.springframework.data.repository.query.Param("limit") int limit,
		@org.springframework.data.repository.query.Param("offset") long offset);

	/** 복구 전용: 삭제 필터를 우회하고 대상 행을 잠근다. */
	@Query(value = "select * from comment where id = :id for update", nativeQuery = true)
	java.util.Optional<Comment> findForRestoration(@org.springframework.data.repository.query.Param("id") Long id);

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Comment c where c.id = :id and c.deletedAt is null")
	java.util.Optional<Comment> findForDeletion(@org.springframework.data.repository.query.Param("id") Long id);
	@Query("""
	SELECT c
	FROM Comment c
	JOIN FETCH c.member
	WHERE c.post.id = :postId
""")
	List<Comment> findAllByPostId(UUID postId , Pageable page);
}
