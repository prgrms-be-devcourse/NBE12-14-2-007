package com.team007.room_escape.domain.post.dto;

import com.team007.room_escape.domain.member.infra.entity.Member;
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

	// TODO 나중에 Member에서 DTO로 만들어서 사용 (현재 Post, Comment에서 공통적으로 사용중)
	@Builder
	@Schema(name = "PostMemberInfo", description = "후기 작성자")
	public record MemberInfo(
		UUID id,
		String nickname,
		String profileImg
	) {

		/** 탈퇴 회원은 닉네임·프로필·id를 가린다. Member엔 @SQLRestriction이 없어서 여기서 가려야 한다. */
		public static MemberInfo from(Post post, ImageUrlResolver imageUrlResolver) {
			Member member = post.getMember();

			if (member == null || member.isDeleted()) {
				return MemberInfo.builder()
					.nickname(Member.WITHDRAWN_NICKNAME)
					.build();
			}

			return MemberInfo.builder()
				.id(member.getId())
				.nickname(member.getNickname())
				.profileImg(imageUrlResolver.resolve(member.getProfileImg()))
				.build();
		}
	}

	@Builder
	@Schema(name = "PostListItem", description = "후기 목록 항목")
	public record ListItem(
		UUID id,
		MemberInfo member,
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
				.member(MemberInfo.from(post, imageUrlResolver))
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
		MemberInfo member,
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
				.member(MemberInfo.from(post, imageUrlResolver))
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
		MemberInfo member,
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
				.member(MemberInfo.from(post, imageUrlResolver))
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
