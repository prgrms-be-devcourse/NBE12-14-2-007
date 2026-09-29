package com.team007.room_escape.domain.comment.infra.dto;

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
        /**
         * 탈퇴한 회원은 닉네임과 프로필을 가린다.
         *
         * Member 에는 @SQLRestriction 을 걸 수 없어서(글·댓글 연관관계가 깨진다)
         * 탈퇴 회원도 그대로 로딩된다. 그래서 응답을 만드는 이 자리에서 가려야 한다.
         *
         * memberId 도 null 로 준다. 값을 남기면 화면이 없는 프로필로 링크를 걸게 된다.
         */
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
