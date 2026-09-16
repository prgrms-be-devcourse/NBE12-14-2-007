package com.team007.room_escape.domain.comment.infra.repository;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostId(UUID postId);
}
