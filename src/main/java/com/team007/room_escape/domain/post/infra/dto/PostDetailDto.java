package com.team007.room_escape.domain.post.infra.dto;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostDetailDto(
        UUID id,
        String nickname,
        Long festivalId,
        String festivalTitle,
        String title,
        String content,
        LocalDateTime date
) {
    public static PostDetailDto from(Post post) {
        return new PostDetailDto(
                post.getId(),
                post.getMember().getNickname(),
                post.getFestival().getId(),
                post.getFestival().getTitle(),
                post.getTitle(),
                post.getContent(),
                post.getUpdatedAt()
        );
    }
}
