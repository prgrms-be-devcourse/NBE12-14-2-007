package com.team007.room_escape.domain.like.infra.repository;

import com.team007.room_escape.domain.like.infra.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
}
