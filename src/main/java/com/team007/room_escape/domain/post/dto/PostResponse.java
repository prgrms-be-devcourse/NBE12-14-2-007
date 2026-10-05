package com.team007.room_escape.domain.post.dto;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.global.storage.ImageUrlResolver;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO 추후 Builder 방식으로 Refactor

/** 이미지 필드(profileImg, thumbnail)는 DB의 key가 아니라 공개 URL로 내려준다. */
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
        /** 탈퇴 회원은 닉네임·프로필·id를 가린다. Member엔 @SQLRestriction이 없어서 여기서 가려야 한다. */
        public static MemberInfo from(Post post, ImageUrlResolver imageUrlResolver) {
            Member member = post.getMember();

            if (member == null || member.isDeleted()) {
                return new MemberInfo(null, Member.WITHDRAWN_NICKNAME, null);
            }

            return new MemberInfo(
                    member.getId(),
                    member.getNickname(),
                    imageUrlResolver.resolve(member.getProfileImg())
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
            LocalDateTime date,
            boolean likedByMe
    ) {
        public static DetailResponse from(Post post, boolean likedByMe, ImageUrlResolver imageUrlResolver) {
            return new DetailResponse(
                    post.getId(),
                    MemberInfo.from(post, imageUrlResolver),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    post.getContent(),
                    imageUrlResolver.resolve(post.getThumbnail()),
                    post.getUpdatedAt(),
                    likedByMe
            );
        }
    }

    public record AdminListResponse(
            UUID id,
            MemberInfo member,
            Long festivalId,
            String festivalTitle,
            String title,
            String thumbnail,
            LocalDateTime date,
            LocalDateTime deletedAt
    ) {
        public static AdminListResponse from(Post post, ImageUrlResolver imageUrlResolver) {
            return new AdminListResponse(
                    post.getId(),
                    MemberInfo.from(post, imageUrlResolver),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    imageUrlResolver.resolve(post.getThumbnail()),
                    post.getUpdatedAt(),
                    post.getDeletedAt()
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
            LocalDateTime date,
            long likeCount
    ) {
        public static ListResponse from(Post post, long likeCount, ImageUrlResolver imageUrlResolver) {
            return new ListResponse(
                    post.getId(),
                    MemberInfo.from(post, imageUrlResolver),
                    post.getFestival().getId(),
                    post.getFestival().getTitle(),
                    post.getTitle(),
                    imageUrlResolver.resolve(post.getThumbnail()),
                    post.getUpdatedAt(),
                    likeCount
            );
        }
    }

}
