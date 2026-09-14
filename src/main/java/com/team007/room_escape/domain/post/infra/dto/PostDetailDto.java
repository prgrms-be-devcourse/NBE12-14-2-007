package com.team007.room_escape.domain.post.infra.dto;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostDetailDto(
        UUID id,
        UUID memberId,
        Long puFevId,
        String title,
        String content,
        String thumbnail,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        LocalDateTime deleteDate
) {
    public static PostDetailDto from(Post post) {
        return new PostDetailDto(
                post.getId(),
                post.getMember().getId(),
                post.getFestival().getId(),
                post.getTitle(),
                post.getContent(),
                post.getThumbnail(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getDeletedAt()
        );
    }
}
