package com.team007.room_escape.domain.like.infra.repository;

import com.team007.room_escape.domain.like.infra.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, Long> {
    /** 후기 */
    Long countByPostId(UUID postId);
    boolean existsByPostIdAndMemberId(UUID postId, UUID memberId);
    Optional<Like> findByPostIdAndMemberId(UUID postId, UUID memberId);

    /** 행사 */
    Long countByFestivalId(Long festivalId);
    boolean existsByFestivalIdAndMemberId(Long festivalId, UUID memberId);
    Optional<Like> findByFestivalIdAndMemberId(Long festivalId, UUID memberId);
}
