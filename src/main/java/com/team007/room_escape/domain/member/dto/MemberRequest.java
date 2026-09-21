package com.team007.room_escape.domain.member.dto;

import com.team007.room_escape.global.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public class MemberRequest {

	private MemberRequest() {
	}

	/**
	 * 마이페이지 회원 정보 수정 요청.
	 * PATCH라서 보낸 필드만 반영한다. 세 필드 모두 선택이다.
	 */
	@Schema(name = "MemberUpdateRequest", description = "마이페이지 회원 정보 수정 요청")
	public record UpdateMyPage(

		@Size(min = 2, max = 30)
		@Schema(description = "닉네임. 생략하면 변경하지 않는다", example = "축제좋아")
		String nickname,

		@ValidPhone
		@Schema(
			description = "휴대폰 번호. 하이픈 없이 숫자만. 생략하면 변경하지 않고, 빈 문자열이면 지운다",
			example = "01012345678"
		)
		String phone,

		@Size(max = 2048)
		@Schema(
			description = "프로필 이미지 key. 업로드 API(/api/v1/images) 응답의 key를 그대로 넣는다. "
				+ "URL이 아니라 key다. 생략하면 변경하지 않고, 빈 문자열이면 지운다",
			example = "profiles/0befc150-badb-4674-a99d-ded96f03814a.png"
		)
		String profileImg
	) {
	}
}
