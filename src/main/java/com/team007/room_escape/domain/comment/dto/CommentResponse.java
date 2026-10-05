package com.team007.room_escape.domain.comment.dto;

import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class CommentResponse {

	private CommentResponse() {
	}

	/** 댓글 응답. 작성·목록·수정에서 함께 쓴다. */
	@Builder
	@Schema(name = "CommentInfo", description = "후기 댓글")
	public record Info(
		@Schema(description = "댓글 ID", example = "1")
		Long id,

		@Schema(description = "후기 ID")
		UUID postId,

		@Schema(description = "작성자. 탈퇴 회원은 닉네임만 \"탈퇴한 사용자\"")
		MemberResponse.MemberInfo member,

		@Schema(description = "댓글 내용", example = "행사 정말 재미있었어요!")
		String content,

		@Schema(description = "댓글 작성 일시", example = "2026-09-16T12:30:00")
		LocalDateTime date
	) {

		public static Info from(Comment comment, ImageUrlResolver imageUrlResolver) {
			return Info.builder()
				.id(comment.getId())
				.postId(comment.getPost().getId())
				.member(MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver))
				.content(comment.getContent())
				.date(comment.getUpdatedAt())
				.build();
		}
	}
}
