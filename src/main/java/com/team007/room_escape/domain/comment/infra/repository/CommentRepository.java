package com.team007.room_escape.domain.comment.infra.repository;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
    SELECT c
    FROM Comment c
    JOIN FETCH c.member
    WHERE c.post.id = :postId
""")
    List<Comment> findAllByPostId(UUID postId , Pageable page);
}
