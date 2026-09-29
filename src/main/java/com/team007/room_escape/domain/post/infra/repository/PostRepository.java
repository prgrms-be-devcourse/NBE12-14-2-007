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
            countQuery = """
                SELECT COUNT(p)
                FROM Post p
                JOIN p.member
                LEFT JOIN p.festival
                WHERE p.deletedAt IS NULL
                """
    )
    Page<Post> findAllNotDeleted(Pageable page);
}
