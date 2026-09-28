package com.team007.room_escape.domain.auth.dto;

public record TokenPair(
	String accessToken,
	String refreshToken
) {
}
