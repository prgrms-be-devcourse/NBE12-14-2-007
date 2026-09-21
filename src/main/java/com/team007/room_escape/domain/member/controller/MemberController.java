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
import org.springframework.web.bind.annotation.PostMapping;
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

	@Operation(
		summary = "비밀번호 변경 인증 코드 발송",
		description = "가입할 때 등록한 이메일로 6자리 인증 코드를 보낸다. "
			+ "받는 주소는 요청으로 받지 않고 서버가 DB에서 읽는다. 유효시간 5분, 재발송은 60초 뒤부터 가능하다."
	)
	@PostMapping("/me/password/verification-code")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> sendPasswordChangeCode(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		memberService.sendPasswordChangeCode(principal.getId());

		return ResponseEntity.ok(ApiResponse.noContentSuccess("인증 코드를 메일로 보냈습니다."));
	}

	@Operation(
		summary = "비밀번호 변경 인증 코드 확인",
		description = "메일로 받은 6자리 코드가 맞는지 확인한다. 통과하면 5분 동안 인증 상태가 유지되며, "
			+ "그 사이에 비밀번호 변경 API를 호출하면 된다."
	)
	@PostMapping("/me/password/verify")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> verifyPasswordChangeCode(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody MemberRequest.VerifyPassword request
	) {
		memberService.verifyPasswordChangeCode(principal.getId(), request);

		return ResponseEntity.ok(ApiResponse.noContentSuccess("인증되었습니다. 새 비밀번호를 입력해 주세요."));
	}

	@Operation(
		summary = "비밀번호 변경",
		description = "인증 코드 확인을 통과한 상태에서 새 비밀번호를 설정한다. "
			+ "변경에 성공하면 Refresh Token이 삭제되므로 모든 기기에서 다시 로그인해야 한다."
	)
	@PatchMapping("/me/password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> changePassword(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody MemberRequest.ChangePassword request
	) {
		memberService.changePassword(principal.getId(), request);

		return ResponseEntity.ok(ApiResponse.noContentSuccess("비밀번호가 변경되었습니다. 다시 로그인해 주세요."));
	}
}
