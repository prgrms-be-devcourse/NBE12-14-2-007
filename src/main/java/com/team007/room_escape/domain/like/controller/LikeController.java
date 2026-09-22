package com.team007.room_escape.domain.like.controller;

import com.team007.room_escape.domain.like.infra.dto.LikeResponse;
import com.team007.room_escape.domain.like.infra.entity.Like;
import com.team007.room_escape.domain.like.service.LikeService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<ApiResponse<LikeResponse>> createPostLike(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
			) {
		LikeResponse response = likeService.createPostLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// TODO 추후에 API따로 작성안하고 개수 조회는 Post로 이동 시킬지 결정

	@Operation(summary = "후기 좋아요 개수 조회", description = "특정 후기의 좋아요 개수를 조회합니다.")
	@GetMapping("/posts/{id}/likes")
	public ResponseEntity<ApiResponse<Long>> getPostLikeCount(@PathVariable UUID id) {
		long likeCount = likeService.getPostLikeCount(id);

		return ResponseEntity.ok(ApiResponse.success(likeCount));
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
	public ResponseEntity<ApiResponse<LikeResponse>> createFestivalLike(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		LikeResponse response = likeService.createFestivalLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "행사 좋아요 개수 조회", description = "특정 행사의 좋아요 개수를 조회합니다.")
	@GetMapping("/festivals/{id}/likes")
	public ResponseEntity<ApiResponse<Long>> getFestivalLikeCount(@PathVariable Long id) {
		long likeCount = likeService.getFestivalLikeCount(id);

		return ResponseEntity.ok(ApiResponse.success(likeCount));
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
