package com.team007.room_escape.domain.like.infra.repository;

import com.team007.room_escape.domain.like.infra.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Long countByPostId(UUID postId);
    boolean existsByPostIdAndMemberId(UUID postId, UUID memberId);
}
