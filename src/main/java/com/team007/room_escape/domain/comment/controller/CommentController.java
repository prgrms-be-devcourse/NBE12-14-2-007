package com.team007.room_escape.domain.comment.controller;

import com.team007.room_escape.domain.comment.infra.dto.CommentRequest;
import com.team007.room_escape.domain.comment.infra.dto.CommentResponse;
import com.team007.room_escape.domain.comment.service.CommentService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@Operation(summary = "댓글 작성", description = "특정 후기에 댓글을 작성합니다.")
	@PostMapping("/posts/{id}/comments")
	public ResponseEntity<ApiResponse<CommentResponse.CommentInfo>> createComment(
			@PathVariable UUID id,
			@Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		CommentResponse.CommentInfo response = commentService.createComment(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 목록", description = "특정 후기에 댓글 목록을 조회합니다.")
	@GetMapping("/posts/{id}/comments")
	public ResponseEntity<ApiResponse<List<CommentResponse.CommentInfo>>> getComments(@PathVariable UUID id) {

		List<CommentResponse.CommentInfo> response = commentService.getComments(id);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
	@PatchMapping("/comments/{id}")
	public ResponseEntity<ApiResponse<CommentResponse.CommentInfo>> updateComment(
			@PathVariable Long id,
			@Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		CommentResponse.CommentInfo response = commentService.updateComment(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 삭제", description = "작성자 본인 또는 관리자가 댓글을 삭제합니다.")
	@DeleteMapping("/comments/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		commentService.deleteComment(id, user.getId(), user.isAdmin());

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
