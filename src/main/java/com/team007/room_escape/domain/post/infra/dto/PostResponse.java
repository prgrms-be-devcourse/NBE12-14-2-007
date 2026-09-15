package com.team007.room_escape.domain.post.infra.dto;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.time.LocalDateTime;
import java.util.UUID;

public class PostResponse {

    public record CreateResponse(
            UUID id
    ) {
        public static CreateResponse from(Post post) {
            return new CreateResponse(post.getId());
        }
    }

    public record DetailResponse(
            UUID id,
            String nickname,
            Long festivalId,
            String festivalTitle,
            String title,
            String content,
            LocalDateTime date
    ) {
        public static DetailResponse from(Post post) {
            return new DetailResponse(
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

    public record ListResponse(
            UUID id,
            String nickname,
            Long festivalId,
            String festivalTitle,
            String title,
            LocalDateTime date
    ) {
        public static ListResponse from(Post post) {
            return new ListResponse(
                    post.getId(),
                    post.getMember().getNickname(),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    post.getUpdatedAt()
            );
        }
    }

}