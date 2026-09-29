package com.team007.room_escape.domain.post.infra.repository;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.util.Optional;
import java.util.UUID;

import com.team007.room_escape.domain.post.type.PostSearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member
        JOIN FETCH p.festival
        WHERE p.festival.id = :festivalId
          AND p.deletedAt IS NULL
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member
        JOIN p.festival
        WHERE p.festival.id = :festivalId
          AND p.deletedAt IS NULL
        """
    )
    Page<Post> findAllByFestivalId(@Param("festivalId") Long festivalId, Pageable page);
    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member m
        JOIN FETCH p.festival f
        WHERE p.deletedAt IS NULL
          AND (
               (:#{#type.name()} = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member m
        JOIN p.festival f
        WHERE p.deletedAt IS NULL
          AND (
               (:#{#type.name()} = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        """
    )
    Page<Post> searchPosts(
            @Param("type") PostSearchType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member
        JOIN FETCH p.festival
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        """
    )
    Page<Post> findAllIncludingDeleted(Pageable page);
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member m
        LEFT JOIN FETCH p.festival f
        WHERE (
               (:#{#type.name()} = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member m
        LEFT JOIN p.festival f
        WHERE (
               (:#{#type.name()} = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:#{#type.name()} = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        """
    )
    Page<Post> searchPostsIncludingDeleted(
            @Param("type") PostSearchType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    @Query(
            value = """
                SELECT p
                FROM Post p
                JOIN FETCH p.member
                LEFT JOIN FETCH p.festival
                WHERE p.deletedAt IS NULL
                """,
            countQuery = """
                SELECT COUNT(p)
                FROM Post p
                JOIN p.member
                LEFT JOIN p.festival
                WHERE p.deletedAt IS NULL
                """
    )
    Page<Post> findAllNotDeleted(Pageable page);

    /**
     * 좋아요순 후기 목록. 전체 목록·검색·행사별 목록을 플래그로 한 쿼리에서 처리한다.
     *
     * 좋아요 수를 후기마다 서브쿼리로 세면 후기 수만큼 반복 실행되고,
     * like 를 그대로 JOIN 후 GROUP BY 하면 행이 좋아요 수만큼 불어나서 둘 다 느리다.
     * 그래서 like 를 후기별로 먼저 한 번 집계한 뒤 붙인다.
     * 동점이면 최신 작성순, 그래도 같으면 id 로 순서를 고정해 페이지가 넘어갈 때 섞이지 않게 한다.
     *
     * TODO: PostgreSQL의 null 파라미터 타입 오류로 사용 중인 hasXxx 플래그를 QueryDSL 도입 시 제거
     */
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member m
        LEFT JOIN FETCH p.festival f
        LEFT JOIN (
            SELECT l.post.id AS postId, COUNT(l) AS likeCount
            FROM Like l
            WHERE l.post IS NOT NULL
            GROUP BY l.post.id
        ) lc ON lc.postId = p.id
        WHERE p.deletedAt IS NULL
          AND (:hasFestival = false OR f.id = :festivalId)
          AND (:hasKeyword = false
            OR (:searchType = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        ORDER BY COALESCE(lc.likeCount, 0) DESC, p.createdAt DESC, p.id DESC
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member m
        LEFT JOIN p.festival f
        WHERE p.deletedAt IS NULL
          AND (:hasFestival = false OR f.id = :festivalId)
          AND (:hasKeyword = false
            OR (:searchType = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        """
    )
    Page<Post> findAllOrderByLikeCount(
            @Param("hasFestival") boolean hasFestival,
            @Param("festivalId") Long festivalId,
            @Param("hasKeyword") boolean hasKeyword,
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
