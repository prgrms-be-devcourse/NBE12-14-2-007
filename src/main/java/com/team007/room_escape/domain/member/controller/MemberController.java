package com.team007.room_escape.domain.member.controller;

import com.team007.room_escape.domain.member.dto.MeResponse;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

	@GetMapping("/me")
	public ApiResponse<MeResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
		return ApiResponse.success(new MeResponse(
			principal.getId(),
			principal.getNickname(),
			principal.getRole(),
			principal.getProfileImg()
		));
	}
}
