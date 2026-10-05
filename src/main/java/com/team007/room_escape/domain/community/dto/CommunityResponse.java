package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class CommunityResponse {

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

	/** 커뮤니티 댓글. 작성·목록·수정에서 함께 쓴다. */
	@Builder
	@Schema(name = "CommunityCommentInfo", description = "커뮤니티 댓글")
	public record CommentInfo(
		Long id,
		UUID postId,
		MemberResponse.MemberInfo member,
		String content,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {

		public static CommentInfo from(CommunityComment comment, ImageUrlResolver imageUrlResolver) {
			return CommentInfo.builder()
				.id(comment.getId())
				.postId(comment.getPost().getId())
				.member(MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver))
				.content(comment.getContent())
				.createdAt(comment.getCreatedAt())
				.updatedAt(comment.getUpdatedAt())
				.build();
		}
	}
}
