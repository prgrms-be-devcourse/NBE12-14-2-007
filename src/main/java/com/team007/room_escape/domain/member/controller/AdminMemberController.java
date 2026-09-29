package com.team007.room_escape.domain.member.controller;

import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team007.room_escape.domain.member.dto.AdminMemberRequest;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.service.AdminMemberService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Member", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

	private final AdminMemberService adminMemberService;

	@Operation(
		summary = "[ADMIN] 회원 목록 조회",
		description = """
			닉네임·이메일 검색어와 등급으로 회원을 검색한다. 조건을 비우면 전체를 조회한다.
			검색어는 닉네임 또는 이메일에 부분 일치하며 대소문자를 구분하지 않는다.
			includeDeleted=true 면 탈퇴 회원도 함께 조회한다.
			기본 정렬은 가입 최신순이며, sort 파라미터로 nickname, email, role 정렬도 쓸 수 있다.
			(예: sort=nickname,asc)
			"""
	)
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<MemberResponse.AdminInfo>>> search(
		@ParameterObject @ModelAttribute AdminMemberRequest.Search request,
		@ParameterObject
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		Page<MemberResponse.AdminInfo> response = adminMemberService.search(request, pageable);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "[ADMIN] 회원 단건 조회",
		description = "회원 한 명의 정보를 조회한다. 탈퇴 회원도 조회된다. 없으면 404(MEMBER000)."
	)
	@GetMapping("/{memberId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<MemberResponse.AdminInfo>> get(
		@PathVariable("memberId") UUID memberId
	) {
		return ResponseEntity.ok(ApiResponse.success(adminMemberService.get(memberId)));
	}

	@Operation(
		summary = "[ADMIN] 회원 등급 변경",
		description = """
			회원의 신뢰 등급을 변경한다. 다음 네 가지는 거부된다.
			본인 등급 변경(403, MEMBER006),
			ROLE_ADMIN 부여(403, MEMBER005),
			관리자 계정의 등급 변경(403, MEMBER004),
			탈퇴 회원의 등급 변경(409, MEMBER007).
			"""
	)
	@PatchMapping("/{memberId}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<MemberResponse.AdminInfo>> changeRole(
		@PathVariable("memberId") UUID memberId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody AdminMemberRequest.ChangeRole request
	) {
		MemberResponse.AdminInfo response =
			adminMemberService.changeRole(principal.getId(), memberId, request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
