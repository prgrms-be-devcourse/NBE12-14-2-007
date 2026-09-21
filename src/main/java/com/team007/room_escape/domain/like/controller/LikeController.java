package com.team007.room_escape.domain.like.controller;

import com.team007.room_escape.domain.like.infra.dto.LikeResponse;
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
@RequestMapping("/api/v1/posts/{id}/likes")
@RequiredArgsConstructor
public class LikeController {

	private final LikeService likeService;

	@Operation(summary = "후기 좋아요 등록", description = "특정 후기에 좋아요를 등록합니다.")
	@PostMapping("/me")
	public ResponseEntity<ApiResponse<LikeResponse>> createLike(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
			) {
		LikeResponse response = likeService.createLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// TODO 추후에 API따로 작성안하고 개수 조회는 Post로 이동 시킬지 결정

	@Operation(summary = "후기 좋아요 개수 조회", description = "특정 후기의 좋아요 개수를 조회합니다.")
	@GetMapping()
	public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable UUID id) {
		long likeCount = likeService.getLikeCount(id);

		return ResponseEntity.ok(ApiResponse.success(likeCount));
	}

	@Operation(summary = "후기 좋아요 삭제", description = "특정 후기에 좋아요를 취소합니다.")
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<LikeResponse>> deleteLike(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		LikeResponse response = likeService.deleteLike(id, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}


}
