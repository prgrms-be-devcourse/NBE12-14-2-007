package com.team007.room_escape.domain.like.infra.repository;

import com.team007.room_escape.domain.like.infra.dto.FestivalLikeCount;
import com.team007.room_escape.domain.like.infra.dto.PostLikeCount;
import com.team007.room_escape.domain.like.infra.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, Long> {
    /** 후기 */
    Long countByPostId(UUID postId);
    boolean existsByPostIdAndMemberId(UUID postId, UUID memberId);
    Optional<Like> findByPostIdAndMemberId(UUID postId, UUID memberId);

    /** 목록 한 페이지에 담긴 후기들의 좋아요 수를 한 번에 센다. 좋아요가 없는 후기는 결과에 없다. */
    @Query("""
        SELECT new com.team007.room_escape.domain.like.infra.dto.PostLikeCount(l.post.id, COUNT(l))
        FROM Like l
        WHERE l.post.id IN :postIds
        GROUP BY l.post.id
        """)
    List<PostLikeCount> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    /** 행사 */
    Long countByFestivalId(Long festivalId);
    boolean existsByFestivalIdAndMemberId(Long festivalId, UUID memberId);
    Optional<Like> findByFestivalIdAndMemberId(Long festivalId, UUID memberId);

    /** 목록 한 페이지에 담긴 행사들의 좋아요 수를 한 번에 센다. 좋아요가 없는 행사는 결과에 없다. */
    @Query("""
        SELECT new com.team007.room_escape.domain.like.infra.dto.FestivalLikeCount(l.festival.id, COUNT(l))
        FROM Like l
        WHERE l.festival.id IN :festivalIds
        GROUP BY l.festival.id
        """)
    List<FestivalLikeCount> countByFestivalIds(@Param("festivalIds") Collection<Long> festivalIds);

    /** 회원이 작성한 삭제되지 않은 행사 제보들이 받은 좋아요 합계 */
    @Query("""
        SELECT COUNT(l)
        FROM Like l
        JOIN l.festival f
        WHERE f.member.id = :authorId
          AND f.providerType = com.team007.room_escape.domain.festival.infra.entity.ProviderType.MEMBER
          AND f.deletedAt IS NULL
          AND l.member.id <> f.member.id
        """)
    long countReceivedFestivalLikesByAuthorId(@Param("authorId") UUID authorId);
}
