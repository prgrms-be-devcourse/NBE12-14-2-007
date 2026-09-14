package com.team007.room_escape.domain.member.dto;

import java.util.UUID;

public record MeResponse(
	UUID id,
	String nickname,
	String role,
	String profileImg
) {
}
