package com.team007.room_escape.domain.community.dto;

import com.team007.room_escape.domain.community.infra.entity.CommunityComment;
import com.team007.room_escape.domain.community.infra.entity.CommunityPost;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.time.LocalDateTime;
import java.util.UUID;

public class CommunityResponse {

	public record MemberInfo(
		UUID id,
		String nickname,
		String profileImg,
		MemberRole role
	) {
		public static MemberInfo from(Member member) {
			if (member == null || member.isDeleted()) {
				return new MemberInfo(null, Member.WITHDRAWN_NICKNAME, null, null);
			}
			return new MemberInfo(
				member.getId(),
				member.getNickname(),
				member.getProfileImg(),
				member.getRole()
			);
		}
	}

	public record PostSummary(
		UUID id,
		CommunityCategory category,
		String title,
		MemberInfo member,
		long viewCount,
		long commentCount,
		LocalDateTime createdAt
	) {
		public static PostSummary from(CommunityPost post, long commentCount) {
			return new PostSummary(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				MemberInfo.from(post.getMember()),
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
		MemberInfo member,
		long viewCount,
		long commentCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		public static PostDetail from(CommunityPost post, long commentCount) {
			return new PostDetail(
				post.getId(),
				post.getCategory(),
				post.getTitle(),
				post.getContent(),
				MemberInfo.from(post.getMember()),
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
		MemberInfo member,
		String content,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		public static CommentInfo from(CommunityComment comment) {
			return new CommentInfo(
				comment.getId(),
				comment.getPost().getId(),
				MemberInfo.from(comment.getMember()),
				comment.getContent(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
			);
		}
	}
}
