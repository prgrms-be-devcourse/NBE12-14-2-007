package com.team007.room_escape.domain.member.dto;

import com.team007.room_escape.global.validation.ValidPassword;
import com.team007.room_escape.global.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

	/** 비밀번호 변경 1단계: 메일로 받은 인증 코드가 맞는지만 확인한다. */
	@Schema(name = "PasswordVerifyRequest", description = "비밀번호 변경 인증 코드 확인 요청")
	public record VerifyPassword(

		@NotBlank
		@Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자입니다.")
		@Schema(description = "메일로 받은 6자리 인증 코드", example = "042913")
		String code
	) {
	}

	/** 비밀번호 변경 2단계: 인증을 통과한 상태에서 새 비밀번호를 설정한다. */
	@Schema(name = "PasswordChangeRequest", description = "새 비밀번호 설정 요청")
	public record ChangePassword(

		@NotBlank
		@ValidPassword
		@Schema(description = "새 비밀번호. 8~25자이며 영문, 숫자, 특수문자(!@#%^&*)를 포함", example = "NewPassword1!")
		String newPassword
	) {
	}

	/**
	 * 회원 탈퇴 요청.
	 *
	 * 비밀번호를 다시 받는 이유: 자리를 비운 사이 남이 브라우저를 만지거나
	 * CSRF로 탈퇴가 호출되는 것을 막기 위해서다. 되돌리기 어려운 작업이라
	 * 로그인 상태만으로는 부족하다.
	 *
	 * @ValidPassword 를 걸지 않는다. 형식 규칙이 나중에 바뀌면
	 * 예전 규칙으로 가입한 회원이 탈퇴하지 못하게 된다.
	 */
	@Schema(name = "MemberWithdrawRequest", description = "회원 탈퇴 요청")
	public record Withdraw(

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		@Schema(description = "본인 확인용 현재 비밀번호", example = "Password1!")
		String password
	) {
	}
}
