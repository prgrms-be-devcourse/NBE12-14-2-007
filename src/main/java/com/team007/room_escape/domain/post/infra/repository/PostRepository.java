package com.team007.room_escape.domain.post.infra.repository;

import com.team007.room_escape.domain.post.infra.entity.Post;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {
}
