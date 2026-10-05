package com.team007.room_escape.domain.member.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.global.storage.ImageUrlResolver;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;

public class MemberResponse {

	private MemberResponse() {
	}

	/** 마이페이지 응답. 본인만 보므로 개인정보를 포함한다. password는 절대 내려보내지 않는다. */
	@Builder
	public record MyPageInfo(
		UUID id,
		String email,
		String nickname,
		String profileImg,
		String phone,
		MemberRole role,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {

		public static MyPageInfo from(Member member, String profileImgUrl) {
			return MyPageInfo.builder()
				.id(member.getId())
				.email(member.getEmail())
				.nickname(member.getNickname())
				.profileImg(profileImgUrl)
				.phone(member.getPhone())
				.role(member.getRole())
				.createdAt(member.getCreatedAt())
				.updatedAt(member.getUpdatedAt())
				.build();
		}
	}

	/** 관리자 화면 회원 정보. 목록·단건·등급 변경에서 함께 쓴다. */
	@Builder
	public record AdminInfo(
		UUID id,
		String email,
		String nickname,
		/** R2 key를 변환한 공개 URL. 이미지가 없으면 null */
		String profileImg,
		String phone,
		MemberRole role,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		/** 탈퇴 시각. 탈퇴하지 않았으면 null */
		LocalDateTime deletedAt
	) {

		public static AdminInfo from(Member member, String profileImgUrl) {
			return AdminInfo.builder()
				.id(member.getId())
				.email(member.getEmail())
				.nickname(member.getNickname())
				.profileImg(profileImgUrl)
				.phone(member.getPhone())
				.role(member.getRole())
				.createdAt(member.getCreatedAt())
				.updatedAt(member.getUpdatedAt())
				.deletedAt(member.getDeletedAt())
				.build();
		}
	}

	/** 다른 도메인 응답에 붙는 작성자 정보. 탈퇴 회원은 닉네임만 "탈퇴한 사용자"로 내려준다. */
	@Builder
	@Schema(name = "MemberInfo", description = "작성자 정보")
	public record MemberInfo(
		UUID id,
		String nickname,
		MemberRole role,
		String profileImg
	) {

		/** Member엔 @SQLRestriction이 없어서 탈퇴 회원도 로딩되므로 여기서 가린다. */
		public static MemberInfo from(Member member, ImageUrlResolver imageUrlResolver) {
			if (member == null || member.isDeleted()) {
				return MemberInfo.builder()
					.nickname(Member.WITHDRAWN_NICKNAME)
					.build();
			}

			return MemberInfo.builder()
				.id(member.getId())
				.nickname(member.getNickname())
				.role(member.getRole())
				.profileImg(imageUrlResolver.resolve(member.getProfileImg()))
				.build();
		}
	}
}
