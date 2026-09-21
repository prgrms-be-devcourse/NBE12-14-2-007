package com.team007.room_escape.domain.post.infra.repository;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @Query(value = """
            SELECT p FROM Post p JOIN FETCH p.member JOIN FETCH p.festival f
            WHERE p.deletedAt IS NULL AND f.deletedAt IS NULL
            """, countQuery = """
            SELECT COUNT(p) FROM Post p JOIN p.festival f
            WHERE p.deletedAt IS NULL AND f.deletedAt IS NULL
            """)
    Page<Post> findAllVisible(Pageable page);

    @Query("""
            SELECT p
            FROM Post p
            JOIN FETCH p.member
            JOIN FETCH p.festival
            WHERE p.festival.id = :festivalId
            """)
    Page<Post> findAllByFestivalId(Long festivalId, Pageable page);
}
