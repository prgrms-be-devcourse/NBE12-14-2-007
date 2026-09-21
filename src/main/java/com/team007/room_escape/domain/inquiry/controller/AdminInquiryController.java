package com.team007.room_escape.domain.inquiry.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team007.room_escape.domain.inquiry.dto.AdminInquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.AdminInquiryResponse;
import com.team007.room_escape.domain.inquiry.service.AdminInquiryService;
import com.team007.room_escape.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Inquiry", description = "관리자 문의·신고 API")
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

	private final AdminInquiryService adminInquiryService;

	@Operation(
		summary = "[ADMIN] 문의·신고 목록 조회",
		description = """
			제목, 답변 상태, 문의 종류로 검색한다. 조건을 비우면 전체를 조회한다.
			includeDeleted=true 면 삭제된 문의도 함께 조회한다.
			기본 정렬은 등록 최신순이며, sort 파라미터로 status, category 정렬도 쓸 수 있다.
			(예: sort=status,asc)
			"""
	)
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AdminInquiryResponse.ListItem>>> search(
		@ParameterObject @ModelAttribute AdminInquiryRequest.Search request,
		@ParameterObject
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		Page<AdminInquiryResponse.ListItem> response = adminInquiryService.search(request, pageable);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
