package com.team007.room_escape.domain.post.dto;

import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.post.infra.entity.Post;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

/** 이미지 필드(profileImg, thumbnail)는 DB의 key가 아니라 공개 URL로 내려준다. */
public class PostResponse {

	private PostResponse() {
	}

	@Schema(name = "PostCreate", description = "후기 작성 결과")
	public record Create(
		UUID id
	) {

		public static Create from(Post post) {
			return new Create(post.getId());
		}
	}

	@Builder
	@Schema(name = "PostListItem", description = "후기 목록 항목")
	public record ListItem(
		UUID id,
		MemberResponse.MemberInfo member,
		Long festivalId,
		String festivalTitle,
		String title,
		String thumbnail,
		LocalDateTime date,
		long likeCount
	) {

		public static ListItem from(Post post, long likeCount, ImageUrlResolver imageUrlResolver) {
			return ListItem.builder()
				.id(post.getId())
				.member(MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver))
				.festivalId(post.getFestival().getId())
				.festivalTitle(post.getFestival().getTitle())
				.title(post.getTitle())
				.thumbnail(imageUrlResolver.resolve(post.getThumbnail()))
				.date(post.getUpdatedAt())
				.likeCount(likeCount)
				.build();
		}
	}

	@Builder
	@Schema(name = "PostAdminListItem", description = "관리자 후기 목록 항목")
	public record AdminListItem(
		UUID id,
		MemberResponse.MemberInfo member,
		Long festivalId,
		String festivalTitle,
		String title,
		String thumbnail,
		LocalDateTime date,
		LocalDateTime deletedAt
	) {

		public static AdminListItem from(Post post, ImageUrlResolver imageUrlResolver) {
			return AdminListItem.builder()
				.id(post.getId())
				.member(MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver))
				.festivalId(post.getFestival().getId())
				.festivalTitle(post.getFestival().getTitle())
				.title(post.getTitle())
				.thumbnail(imageUrlResolver.resolve(post.getThumbnail()))
				.date(post.getUpdatedAt())
				.deletedAt(post.getDeletedAt())
				.build();
		}
	}

	@Builder
	@Schema(name = "PostDetail", description = "후기 상세")
	public record Detail(
		UUID id,
		MemberResponse.MemberInfo member,
		Long festivalId,
		String festivalTitle,
		String title,
		String content,
		String thumbnail,
		LocalDateTime date,
		boolean likedByMe
	) {

		public static Detail from(Post post, boolean likedByMe, ImageUrlResolver imageUrlResolver) {
			return Detail.builder()
				.id(post.getId())
				.member(MemberResponse.MemberInfo.from(post.getMember(), imageUrlResolver))
				.festivalId(post.getFestival().getId())
				.festivalTitle(post.getFestival().getTitle())
				.title(post.getTitle())
				.content(post.getContent())
				.thumbnail(imageUrlResolver.resolve(post.getThumbnail()))
				.date(post.getUpdatedAt())
				.likedByMe(likedByMe)
				.build();
		}
	}
}
