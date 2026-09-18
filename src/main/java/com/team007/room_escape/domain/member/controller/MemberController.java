package com.team007.room_escape.domain.member.controller;

import com.team007.room_escape.domain.member.dto.MemberRequest;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.service.MemberService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@Operation(
		summary = "마이페이지 조회",
		description = "로그인한 본인의 정보를 조회한다. 회원 식별자는 토큰에서 꺼내므로 "
			+ "남의 정보를 조회할 수 없고, 탈퇴한 계정이면 404로 응답한다."
	)
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<MemberResponse.MyPageInfo>> getMyPage(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		MemberResponse.MyPageInfo response = memberService.getMyPage(principal.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "마이페이지 수정",
		description = "로그인한 본인의 닉네임·연락처·프로필 이미지를 수정한다. "
			+ "보낸 필드만 반영되며, profileImg는 업로드 API가 돌려준 key를 넣는다. "
			+ "제재 등급(ROLE_WARNING)은 수정할 수 없고 403(MEMBER003)으로 응답한다."
	)
	@PatchMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<MemberResponse.MyPageInfo>> updateMyPage(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody MemberRequest.UpdateMyPage request
	) {
		MemberResponse.MyPageInfo response = memberService.updateMyPage(principal.getId(), request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
