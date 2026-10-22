package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

public class CommunityResponse {

	/** 삭제된 댓글 자리에 내려주는 문구. */
	public static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다";

	private CommunityResponse() {
	}

	@Builder
	@Schema(name = "CommunityPostListItem", description = "커뮤니티 글 목록 항목")
	public record PostListItem(
		UUID id,
		CommunityCategory category,
		String title,
		MemberResponse.MemberInfo member,
		long viewCount,
		long commentCount,
		LocalDateTime createdAt
	) {

		public static PostListItem from(CommunityPost post, long commentCount, ImageUrlResolver imageUrlResolver) {
			return PostListItem.builder()
				.id(post.getId())
				.category(post.getCategory())
				.title(post.getTitle())
				.member(MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver))
				.viewCount(post.getViewCount())
				.commentCount(commentCount)
				.createdAt(post.getCreatedAt())
				.build();
		}
	}

	/** 커뮤니티 글 상세. 작성·수정 응답도 같은 모양이다. */
	@Builder
	@Schema(name = "CommunityPostDetail", description = "커뮤니티 글 상세")
	public record PostDetail(
		UUID id,
		CommunityCategory category,
		String title,
		String content,
		MemberResponse.MemberInfo member,
		long viewCount,
		long commentCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {

		public static PostDetail from(CommunityPost post, long commentCount, ImageUrlResolver imageUrlResolver) {
			return PostDetail.builder()
				.id(post.getId())
				.category(post.getCategory())
				.title(post.getTitle())
				.content(post.getContent())
				.member(MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver))
				.viewCount(post.getViewCount())
				.commentCount(commentCount)
				.createdAt(post.getCreatedAt())
				.updatedAt(post.getUpdatedAt())
				.build();
		}
	}

	/** 커뮤니티 댓글. 작성·목록·수정에서 함께 쓴다. 목록에서는 일반 댓글 아래에 대댓글이 replies로 붙는다. */
	@Builder
	@Schema(name = "CommunityCommentInfo", description = "커뮤니티 댓글")
	public record CommentInfo(
		Long id,
		UUID postId,
		@Schema(description = "부모 댓글 ID. 일반 댓글이면 null", nullable = true)
		Long parentId,
		@Schema(description = "삭제 여부. true면 content는 \"삭제된 댓글입니다\", member는 null")
		boolean deleted,
		@Schema(description = "작성자. 삭제된 댓글은 null", nullable = true)
		MemberResponse.MemberInfo member,
		@Schema(description = "댓글 내용. 삭제된 댓글은 \"삭제된 댓글입니다\"")
		String content,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		@Schema(description = "대댓글. 오래된 순. 대댓글 자신과 작성·수정 응답에서는 빈 배열")
		List<CommentInfo> replies
	) {

		public static CommentInfo from(CommunityComment comment, ImageUrlResolver imageUrlResolver) {
			return of(comment, List.of(), imageUrlResolver);
		}

		public static CommentInfo of(
			CommunityComment comment,
			List<CommunityComment> replies,
			ImageUrlResolver imageUrlResolver
		) {
			boolean deleted = comment.isDeleted();
			return CommentInfo.builder()
				.id(comment.getId())
				.postId(comment.getPost().getId())
				.parentId(comment.isReply() ? comment.getParent().getId() : null)
				.deleted(deleted)
				// 삭제된 댓글은 누가 무엇을 썼는지 내려주지 않는다.
				.member(deleted ? null : MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver))
				.content(deleted ? DELETED_COMMENT_CONTENT : comment.getContent())
				.createdAt(comment.getCreatedAt())
				.updatedAt(comment.getUpdatedAt())
				.replies(replies.stream()
					.map(reply -> from(reply, imageUrlResolver))
					.toList())
				.build();
		}
	}
}
