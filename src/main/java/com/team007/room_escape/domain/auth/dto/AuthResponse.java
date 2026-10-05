package com.team007.room_escape.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthResponse {

	private AuthResponse() {
	}

	/** 로그인·재발급 응답. Refresh Token은 쿠키로 보내고 본문에는 Access Token만 담는다. */
	@Schema(name = "AuthToken", description = "Access Token 응답")
	public record Token(
		@Schema(description = "Access Token")
		String accessToken,

		@Schema(description = "토큰 종류", example = "Bearer")
		String tokenType
	) {

		public static Token bearer(String accessToken) {
			return new Token(accessToken, "Bearer");
		}
	}
}
