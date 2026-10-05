package com.team007.room_escape.domain.post.infra.repository;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.util.Optional;
import java.util.UUID;

import com.team007.room_escape.domain.post.type.PostSearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /** 검색어 없는 목록 개수. 회원은 soft delete라 조인해도 결과가 같아서 조인 없이 센다. (조인 시 1건 0.5초) */
    String COUNT_NOT_DELETED = """
        SELECT COUNT(*)
        FROM Post p
        WHERE p.deletedAt IS NULL
        """;
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
        WHERE p.festival.id = :festivalId
          AND p.deletedAt IS NULL
        """
    )
    Page<Post> findAllByFestivalId(@Param("festivalId") Long festivalId, Pageable page);
    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByIdAndDeletedAtIsNull(UUID id);
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
            countQuery = COUNT_NOT_DELETED
    )
    Page<Post> findAllNotDeleted(Pageable page);

    /** 좋아요순 전체 목록. idx_post_like_count를 타고, 동점은 최신순 → id로 고정한다. */
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member
        LEFT JOIN FETCH p.festival
        WHERE p.deletedAt IS NULL
        ORDER BY p.likeCount DESC, p.createdAt DESC, p.id DESC
        """,
            countQuery = COUNT_NOT_DELETED
    )
    Page<Post> findAllOrderByLikeCount(Pageable pageable);

    /** 좋아요순 검색. 닉네임·행사 제목으로 찾아야 해서 개수도 조인해서 센다. */
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member m
        LEFT JOIN FETCH p.festival f
        WHERE p.deletedAt IS NULL
          AND ((:searchType = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        ORDER BY p.likeCount DESC, p.createdAt DESC, p.id DESC
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member m
        LEFT JOIN p.festival f
        WHERE p.deletedAt IS NULL
          AND ((:searchType = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:searchType = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        """
    )
    Page<Post> searchPostsOrderByLikeCount(
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /** 행사별 좋아요순 목록. 플래그로 합치면 행사별 인덱스를 못 탈 수 있어서 따로 둔다. */
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member
        JOIN FETCH p.festival f
        WHERE f.id = :festivalId
          AND p.deletedAt IS NULL
        ORDER BY p.likeCount DESC, p.createdAt DESC, p.id DESC
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        WHERE p.festival.id = :festivalId
          AND p.deletedAt IS NULL
        """
    )
    Page<Post> findAllByFestivalIdOrderByLikeCount(@Param("festivalId") Long festivalId, Pageable pageable);

    /** 좋아요 수 1 증가. DB에서 바로 더해서 동시에 눌러도 빠지지 않는다. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int increaseLikeCount(@Param("id") UUID id);

    /** 좋아요 수 1 감소. 0 아래로는 내려가지 않는다. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    int decreaseLikeCount(@Param("id") UUID id);

    /** 영속성 컨텍스트에 올라온 엔티티 대신 DB의 최신 좋아요 수를 읽는다. (삭제된 후기 포함) */
    @Query("SELECT p.likeCount FROM Post p WHERE p.id = :id")
    Optional<Long> findLikeCountById(@Param("id") UUID id);
}
