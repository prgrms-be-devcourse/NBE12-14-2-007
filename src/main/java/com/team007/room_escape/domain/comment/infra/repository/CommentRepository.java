package com.team007.room_escape.domain.comment.infra.repository;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
