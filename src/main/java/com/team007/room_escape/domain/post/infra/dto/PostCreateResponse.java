package com.team007.room_escape.domain.post.infra.dto;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.util.UUID;

public record PostCreateResponse(UUID id) {
    public static PostCreateResponse from(Post post) {
        return new PostCreateResponse(post.getId());
    }
}
