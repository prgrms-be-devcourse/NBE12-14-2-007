package com.team007.room_escape.domain.comment.dto;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommentResponse {
    public record CommentInfo(
            @Schema(description = "댓글 ID", example = "1")
            Long id,
            @Schema(description = "후기 ID")
            UUID postId,
            @Schema(description = "작성자 ID")
            UUID memberId,
            @Schema(description = "작성자 닉네임")
            String nickname,
            @Schema(description = "작성자 프로필 이미지 공개 URL")
            String profile_img,
            @Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
            String content,
            @Schema(description = "댓글 작성 일시", example = "2026-09-16T12:30:00")
            LocalDateTime date
    ) {
        /** 탈퇴 회원은 닉네임·프로필·memberId를 가린다. Member엔 @SQLRestriction이 없어서 여기서 가려야 한다. */
        public static CommentInfo from(Comment comment, ImageUrlResolver imageUrlResolver) {
            Member member = comment.getMember();
            boolean withdrawn = member == null || member.isDeleted();

            return new CommentInfo(
                    comment.getId(),
                    comment.getPost().getId(),
                    withdrawn ? null : member.getId(),
                    withdrawn ? Member.WITHDRAWN_NICKNAME : member.getNickname(),
                    withdrawn ? null : imageUrlResolver.resolve(member.getProfileImg()),
                    comment.getContent(),
                    comment.getUpdatedAt()
            );
        }

    }
}
