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
			@Valid @RequestBody CommentRequest.CommentCreateRequest request,
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
}
