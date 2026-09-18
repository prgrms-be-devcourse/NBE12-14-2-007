package com.team007.room_escape.domain.post.infra.dto;

import com.team007.room_escape.domain.post.infra.entity.Post;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO 추후 Builder 방식으로 Refactor

public class PostResponse {

    public record CreateResponse(
            UUID id
    ) {
        public static CreateResponse from(Post post) {
            return new CreateResponse(post.getId());
        }
    }

    // TODO 나중에 Member에서 DTO로 만들어서 사용 (현재 Post, Comment에서 공통적으로 사용중)
    public record MemberInfo(
            UUID id,
            String nickname,
            String profileImg
    ) {
        public static MemberInfo from(Post post) {
            return new MemberInfo(
                    post.getMember().getId(),
                    post.getMember().getNickname(),
                    post.getMember().getProfileImg()
            );
        }
    }

    public record DetailResponse(
            UUID id,
            MemberInfo member,
            Long festivalId,
            String festivalTitle,
            String title,
            String content,
            String thumbnail,
            LocalDateTime date
    ) {
        public static DetailResponse from(Post post) {
            return new DetailResponse(
                    post.getId(),
                    MemberInfo.from(post),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    post.getContent(),
                    post.getThumbnail(),
                    post.getUpdatedAt()
            );
        }
    }

    public record ListResponse(
            UUID id,
            MemberInfo member,
            Long festivalId,
            String festivalTitle,
            String title,
            String thumbnail,
            LocalDateTime date
    ) {
        public static ListResponse from(Post post) {
            return new ListResponse(
                    post.getId(),
                    MemberInfo.from(post),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    post.getThumbnail(),
                    post.getUpdatedAt()
            );
        }
    }

}