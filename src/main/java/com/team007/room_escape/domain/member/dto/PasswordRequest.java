package com.team007.room_escape.domain.member.dto;

import com.team007.room_escape.global.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PasswordRequest {

	private PasswordRequest() {
	}

	/** 1단계: 메일로 받은 인증 코드가 맞는지만 확인한다. */
	@Schema(name = "PasswordVerifyRequest", description = "비밀번호 변경 인증 코드 확인 요청")
	public record Verify(

		@NotBlank
		@Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자입니다.")
		@Schema(description = "메일로 받은 6자리 인증 코드", example = "042913")
		String code
	) {
	}

	/** 2단계: 인증을 통과한 상태에서 새 비밀번호를 설정한다. */
	@Schema(name = "PasswordChangeRequest", description = "새 비밀번호 설정 요청")
	public record Change(

		@NotBlank
		@ValidPassword
		@Schema(description = "새 비밀번호. 8~25자이며 영문, 숫자, 특수문자(!@#%^&*)를 포함", example = "NewPassword1!")
		String newPassword
	) {
	}
}
