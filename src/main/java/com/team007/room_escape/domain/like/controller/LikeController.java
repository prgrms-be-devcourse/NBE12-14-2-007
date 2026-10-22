package com.team007.room_escape.domain.like.controller;

import com.team007.room_escape.domain.like.dto.LikeResponse;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.service.LikeService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LikeController {

	private final LikeService likeService;

	/** 후기 */

	@Operation(summary = "후기 좋아요 등록", description = "특정 후기에 좋아요를 등록합니다.")
	@PostMapping("/posts/{id}/likes/me")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<LikeResponse>> createPostLike(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
			) {
		LikeResponse response = likeService.createPostLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "후기 좋아요 취소", description = "특정 후기에 좋아요를 취소합니다.")
	@DeleteMapping("/posts/{id}/likes/me")
	public ResponseEntity<ApiResponse<LikeResponse>> deletePostLike(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		LikeResponse response = likeService.deletePostLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	/** 행사 */
	@Operation(summary = "행사 좋아요 등록", description = "특정 행사에 좋아요를 등록합니다.")
	@PostMapping("/festivals/{id}/likes/me")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<LikeResponse>> createFestivalLike(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		LikeResponse response = likeService.createFestivalLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "행사 좋아요 취소", description = "특정 행사에 좋아요를 취소합니다.")
	@DeleteMapping("/festivals/{id}/likes/me")
	public ResponseEntity<ApiResponse<LikeResponse>> deleteFestivalLike(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		LikeResponse response = likeService.deleteFestivalLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
