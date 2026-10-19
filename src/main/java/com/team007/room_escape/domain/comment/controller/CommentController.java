package com.team007.room_escape.domain.comment.controller;

import com.team007.room_escape.domain.comment.dto.CommentRequest;
import com.team007.room_escape.domain.comment.dto.CommentResponse;
import com.team007.room_escape.domain.comment.service.CommentService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@Operation(summary = "[ADMIN] 후기 댓글 관리 목록", description = "삭제된 댓글과 최신 삭제 사유도 조회합니다. 작성일 내림차순이며 일반 사용자용 목록에는 삭제된 댓글을 노출하지 않습니다.")
	@GetMapping("/admin/posts/{id}/comments")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<CommentResponse.AdminInfo>>> getCommentsForAdmin(
		@PathVariable UUID id,
		@AuthenticationPrincipal CustomUserDetails user,
		@PageableDefault(size = 20) Pageable page
	) {
		return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsForAdmin(id, user.getId(), page)));
	}

	@Operation(summary = "댓글 작성", description = "특정 후기에 댓글을 작성합니다.")
	@PostMapping("/posts/{id}/comments")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommentResponse.Info>> createComment(
			@PathVariable UUID id,
			@Valid @RequestBody CommentRequest.Upsert request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		CommentResponse.Info response = commentService.createComment(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 목록", description = "특정 후기에 댓글 목록을 조회합니다.")
	@GetMapping("/posts/{id}/comments")
	public ResponseEntity<ApiResponse<List<CommentResponse.Info>>> getComments(
			@PathVariable UUID id,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
			Pageable page) {

		List<CommentResponse.Info> response = commentService.getComments(id, page);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
	@PatchMapping("/comments/{id}")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommentResponse.Info>> updateComment(
			@PathVariable Long id,
			@Valid @RequestBody CommentRequest.Upsert request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		CommentResponse.Info response = commentService.updateComment(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "댓글 삭제", description = "작성자 본인이 삭제합니다. 관리자의 타인 댓글 삭제는 공통 관리자 조치 API를 사용합니다.")
	@DeleteMapping("/comments/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
			@PathVariable Long id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		commentService.deleteComment(id, user.getId());

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
