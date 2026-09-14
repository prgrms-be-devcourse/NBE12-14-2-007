package com.team007.room_escape.domain.auth.dto;

import com.team007.room_escape.global.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
	@NotBlank
	@Email
	String email,
	@NotBlank
	@ValidPassword
	String password,
	@NotBlank
	@Size(min = 2, max = 30)
	String nickname,
	@Size(max = 20)
	String phone
) {
}
