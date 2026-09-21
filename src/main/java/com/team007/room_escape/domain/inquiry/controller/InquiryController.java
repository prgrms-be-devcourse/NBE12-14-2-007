package com.team007.room_escape.domain.inquiry.controller;

import com.team007.room_escape.domain.inquiry.dto.InquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.InquiryResponse;
import com.team007.room_escape.domain.inquiry.service.InquiryService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry", description = "문의·신고 API")
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

	private final InquiryService inquiryService;

	@Operation(
		summary = "문의 등록",
		description = "문의 또는 신고를 등록한다. 등급 제한 없이 로그인한 회원이면 누구나 쓸 수 있다. "
			+ "첨부 이미지는 업로드 API가 돌려준 key를 넣는다."
	)
	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<InquiryResponse.Info>> create(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody InquiryRequest.Create request
	) {
		InquiryResponse.Info response = inquiryService.create(principal.getId(), request);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@Operation(
		summary = "[ME] 내 문의 목록 조회",
		description = "로그인한 사용자가 등록한 문의를 최신순으로 조회한다. 남의 문의는 조회할 수 없다."
	)
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<List<InquiryResponse.Info>>> findMine(
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		List<InquiryResponse.Info> response = inquiryService.findMine(principal.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "[ME] 내 문의 상세 조회",
		description = "본인이 등록한 문의를 상세 조회한다. 남의 문의를 조회하면 404로 응답한다."
	)
	@GetMapping("/{inquiryId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<InquiryResponse.Info>> findMineById(
		@PathVariable("inquiryId") UUID inquiryId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		InquiryResponse.Info response = inquiryService.findMineById(inquiryId, principal.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "문의 수정",
		description = "본인이 작성한 문의를 수정한다. 보낸 필드만 반영된다. "
			+ "답변이 달린 뒤에는 수정할 수 없다(409, INQUIRY003)."
	)
	@PatchMapping("/{inquiryId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<InquiryResponse.Info>> update(
		@PathVariable("inquiryId") UUID inquiryId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody InquiryRequest.Update request
	) {
		InquiryResponse.Info response = inquiryService.update(inquiryId, principal.getId(), request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(
		summary = "문의 삭제",
		description = "본인이 작성한 문의를 삭제한다. 관리자는 모든 문의를 삭제할 수 있다. "
			+ "답변 여부와 무관하게 삭제할 수 있다."
	)
	@DeleteMapping("/{inquiryId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> delete(
		@PathVariable("inquiryId") UUID inquiryId,
		@AuthenticationPrincipal CustomUserDetails principal
	) {
		inquiryService.delete(inquiryId, principal.getId(), principal.isAdmin());

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
