package com.team007.room_escape.domain.auth.dto;

public record TokenResponse(
	String accessToken,
	String tokenType
) {
}
