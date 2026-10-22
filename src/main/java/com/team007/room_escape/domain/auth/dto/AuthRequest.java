package com.team007.room_escape.domain.auth.dto;

import com.team007.room_escape.global.validation.ValidPassword;
import com.team007.room_escape.global.validation.ValidPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

	private AuthRequest() {
	}

	@Schema(name = "AuthLoginRequest", description = "로그인 요청")
	public record Login(
		@NotBlank
		@Email
		@Schema(description = "회원 이메일", example = "user@example.com")
		String email,

		@NotBlank
		@Schema(description = "비밀번호", example = "Password1!")
		String password
	) {
	}

	@Schema(name = "AuthSignupRequest", description = "회원가입 요청")
	public record Signup(
		@NotBlank
		@Email
		@Schema(description = "회원 이메일", example = "user@example.com")
		String email,

		@NotBlank
		@ValidPassword
		@Schema(description = "8~15자이며 영문, 숫자, 특수문자(!@#%^&*)를 포함한 비밀번호", example = "Password1!")
		String password,

		@NotBlank
		@Size(min = 2, max = 30)
		@Schema(description = "닉네임", example = "축제좋아")
		String nickname,

		@ValidPhone
		@Schema(description = "휴대폰 번호. 하이픈 없이 숫자만", example = "01012345678")
		String phone
	) {
	}
}
