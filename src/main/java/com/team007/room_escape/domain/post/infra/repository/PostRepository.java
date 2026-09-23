package com.team007.room_escape.domain.post.infra.repository;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
        WHERE p.festival.id = :festivalId
          AND p.deletedAt IS NULL
        """
    )
    Page<Post> findAllByFestivalId(Long festivalId, Pageable page);
    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);
    @Query(
            value = """
        SELECT p
        FROM Post p
        JOIN FETCH p.member m
        JOIN FETCH p.festival f
        WHERE p.deletedAt IS NULL
          AND (
               (:type = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'FESTIVAL_TITLE'
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
               (:type = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          )
        """
    )
    Page<Post> searchPosts(
            String type,
            String keyword,
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
        JOIN FETCH p.festival f
        WHERE (
               (:type = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        """,
            countQuery = """
        SELECT COUNT(p)
        FROM Post p
        JOIN p.member m
        JOIN p.festival f
        WHERE (
               (:type = 'TITLE'
                    AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'MEMBER_NICKNAME'
                    AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (:type = 'FESTIVAL_TITLE'
                    AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        )
        """
    )
    Page<Post> searchPostsIncludingDeleted(
            String type,
            String keyword,
            Pageable pageable
    );
}
