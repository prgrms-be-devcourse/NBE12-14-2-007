package com.team007.room_escape.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank
	@Email
	String email,
	@NotBlank
	String password
) {
}
