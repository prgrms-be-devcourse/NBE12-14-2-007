package com.team007.room_escape.domain.member.controller;

import com.team007.room_escape.domain.member.dto.MyContentResponse;
import com.team007.room_escape.domain.member.service.MyContentService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyContentController {
	private final MyContentService service;
	@GetMapping("/posts")
	@Operation(summary = "내가 작성한 글", description = "행사 제보·후기·커뮤니티 글을 삭제 여부와 관계없이 조회합니다.")
	public ResponseEntity<ApiResponse<Page<MyContentResponse.Item>>> posts(@AuthenticationPrincipal CustomUserDetails user,
		@PageableDefault(size = 20) Pageable page) {
		return ResponseEntity.ok(ApiResponse.success(service.findMine(user.getId(), false, page)));
	}
	@GetMapping("/comments")
	@Operation(summary = "내가 작성한 댓글", description = "후기·커뮤니티 댓글을 삭제 여부와 관계없이 조회합니다.")
	public ResponseEntity<ApiResponse<Page<MyContentResponse.Item>>> comments(@AuthenticationPrincipal CustomUserDetails user,
		@PageableDefault(size = 20) Pageable page) {
		return ResponseEntity.ok(ApiResponse.success(service.findMine(user.getId(), true, page)));
	}
}
