package com.team007.room_escape.domain.comment.dto;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.dto.DeletionHistoryResponse;
import com.team007.room_escape.domain.comment.infra.entity.Comment;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

public class CommentResponse {

	/** 삭제된 댓글 자리에 내려주는 문구. */
	public static final String DELETED_CONTENT = "삭제된 댓글입니다";

	private CommentResponse() {
	}

	@Builder
	@Schema(name = "AdminCommentInfo", description = "삭제된 댓글과 최신 삭제 사유를 포함한 관리자용 댓글")
	public record AdminInfo(
		Long id,
		UUID postId,
		Long parentId,
		MemberResponse.MemberInfo member,
		String content,
		LocalDateTime date,
		LocalDateTime deletedAt,
		boolean parentDeleted,
		DeletionHistoryResponse.Latest deletion,
		List<AdminInfo> replies
	) {
		public static AdminInfo from(
			Comment comment,
			ImageUrlResolver imageUrlResolver,
			boolean parentDeleted,
			DeletionHistory history,
			List<AdminInfo> replies
		) {
			return AdminInfo.builder()
				.id(comment.getId())
				.postId(comment.getPost().getId())
				.parentId(comment.isReply() ? comment.getParent().getId() : null)
				.member(MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver))
				.content(comment.getContent())
				.date(comment.getCreatedAt())
				.deletedAt(comment.getDeletedAt())
				.parentDeleted(parentDeleted)
				.replies(replies)
				.deletion(history == null ? null : DeletionHistoryResponse.Latest.from(history))
				.build();
		}
	}

	/** 댓글 응답. 작성·목록·수정에서 함께 쓴다. 목록에서는 일반 댓글 아래에 대댓글이 replies로 붙는다. */
	@Builder
	@Schema(name = "CommentInfo", description = "후기 댓글")
	public record Info(
		@Schema(description = "댓글 ID", example = "1")
		Long id,

		@Schema(description = "후기 ID")
		UUID postId,

		@Schema(description = "부모 댓글 ID. 일반 댓글이면 null", example = "1", nullable = true)
		Long parentId,

		@Schema(description = "삭제 여부. true면 content는 \"삭제된 댓글입니다\", member는 null")
		boolean deleted,

		@Schema(description = "작성자. 탈퇴 회원은 닉네임만 \"탈퇴한 사용자\", 삭제된 댓글은 null", nullable = true)
		MemberResponse.MemberInfo member,

		@Schema(description = "댓글 내용. 삭제된 댓글은 \"삭제된 댓글입니다\"", example = "행사 정말 재미있었어요!")
		String content,

		@Schema(description = "댓글 작성 일시", example = "2026-09-16T12:30:00")
		LocalDateTime date,

		@Schema(description = "대댓글. 오래된 순. 대댓글 자신과 작성·수정 응답에서는 빈 배열")
		List<Info> replies
	) {

		public static Info from(Comment comment, ImageUrlResolver imageUrlResolver) {
			return of(comment, List.of(), imageUrlResolver);
		}

		public static Info of(Comment comment, List<Comment> replies, ImageUrlResolver imageUrlResolver) {
			boolean deleted = comment.isDeleted();
			return Info.builder()
				.id(comment.getId())
				.postId(comment.getPost().getId())
				.parentId(comment.isReply() ? comment.getParent().getId() : null)
				.deleted(deleted)
				// 삭제된 댓글은 누가 무엇을 썼는지 내려주지 않는다.
				.member(deleted ? null : MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver))
				.content(deleted ? DELETED_CONTENT : comment.getContent())
				.date(comment.getCreatedAt())
				.replies(replies.stream()
					.map(reply -> from(reply, imageUrlResolver))
					.toList())
				.build();
		}
	}
}
