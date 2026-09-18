package com.team007.room_escape.domain.inquiry.controller;

import com.team007.room_escape.domain.inquiry.dto.InquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.InquiryResponse;
import com.team007.room_escape.domain.inquiry.service.InquiryService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	public ResponseEntity<ApiResponse<InquiryResponse.CreateInfo>> create(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody InquiryRequest.Create request
	) {
		InquiryResponse.CreateInfo response = inquiryService.create(principal.getId(), request);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
