package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import java.time.LocalDateTime;
import java.util.UUID;

public class CommunityResponse {

	public record PostSummary(
		UUID id,
		CommunityCategory category,
		String title,
		MemberResponse.MemberInfo member,
		long viewCount,
		long commentCount,
		LocalDateTime createdAt
	) {
		public static PostSummary from(CommunityPost post, long commentCount, ImageUrlResolver imageUrlResolver) {
			return new PostSummary(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver),
				post.getViewCount(),
				commentCount,
				post.getCreatedAt()
			);
		}
	}

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
			return new PostDetail(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				post.getContent(),
				MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver),
				post.getViewCount(),
				commentCount,
				post.getCreatedAt(),
				post.getUpdatedAt()
			);
		}
	}

	public record CommentInfo(
		Long id,
		UUID postId,
		MemberResponse.MemberInfo member,
		String content,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		public static CommentInfo from(CommunityComment comment, ImageUrlResolver imageUrlResolver) {
			return new CommentInfo(
				comment.getId(),
				comment.getPost().getId(),
				MemberResponse.MemberInfo.from(comment.getMember(), imageUrlResolver),
				comment.getContent(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
			);
		}
	}
}
