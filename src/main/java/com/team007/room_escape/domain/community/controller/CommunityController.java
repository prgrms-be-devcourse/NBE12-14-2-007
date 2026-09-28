package com.team007.room_escape.domain.community.controller;

import com.team007.room_escape.domain.community.dto.CommunityRequest;
import com.team007.room_escape.domain.community.dto.CommunityResponse;
import com.team007.room_escape.domain.community.service.CommunityService;
import com.team007.room_escape.domain.community.type.CommunityCategory;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "커뮤니티 게시글과 댓글 API")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

	private final CommunityService communityService;

	@Operation(summary = "커뮤니티 글 목록")
	@GetMapping("/posts")
	public ResponseEntity<ApiResponse<Page<CommunityResponse.PostSummary>>> getPosts(
		@RequestParam(required = false) CommunityCategory category,
		@RequestParam(required = false) String keyword,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.getPosts(category, keyword, pageable)
		));
	}

	@Operation(summary = "커뮤니티 글 상세")
	@GetMapping("/posts/{postId}")
	public ResponseEntity<ApiResponse<CommunityResponse.PostDetail>> getPost(
		@PathVariable UUID postId
	) {
		return ResponseEntity.ok(ApiResponse.success(communityService.getPost(postId)));
	}

	@Operation(summary = "커뮤니티 글 작성")
	@PostMapping("/posts")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommunityResponse.PostDetail>> createPost(
		@Valid @RequestBody CommunityRequest.PostUpsert request,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.createPost(request, user.getId())
		));
	}

	@Operation(summary = "커뮤니티 글 수정")
	@PatchMapping("/posts/{postId}")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommunityResponse.PostDetail>> updatePost(
		@PathVariable UUID postId,
		@Valid @RequestBody CommunityRequest.PostUpsert request,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.updatePost(postId, request, user.getId())
		));
	}

	@Operation(summary = "커뮤니티 글 삭제")
	@DeleteMapping("/posts/{postId}")
	public ResponseEntity<ApiResponse<Void>> deletePost(
		@PathVariable UUID postId,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		communityService.deletePost(postId, user.getId(), user.isAdmin());
		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@Operation(summary = "커뮤니티 댓글 목록")
	@GetMapping("/posts/{postId}/comments")
	public ResponseEntity<ApiResponse<Page<CommunityResponse.CommentInfo>>> getComments(
		@PathVariable UUID postId,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.getComments(postId, pageable)
		));
	}

	@Operation(summary = "커뮤니티 댓글 작성")
	@PostMapping("/posts/{postId}/comments")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommunityResponse.CommentInfo>> createComment(
		@PathVariable UUID postId,
		@Valid @RequestBody CommunityRequest.CommentUpsert request,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.createComment(postId, request, user.getId())
		));
	}

	@Operation(summary = "커뮤니티 댓글 수정")
	@PatchMapping("/comments/{commentId}")
	@PreAuthorize("hasRole('UNVERIFIED')")
	public ResponseEntity<ApiResponse<CommunityResponse.CommentInfo>> updateComment(
		@PathVariable Long commentId,
		@Valid @RequestBody CommunityRequest.CommentUpsert request,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		return ResponseEntity.ok(ApiResponse.success(
			communityService.updateComment(commentId, request, user.getId())
		));
	}

	@Operation(summary = "커뮤니티 댓글 삭제")
	@DeleteMapping("/comments/{commentId}")
	public ResponseEntity<ApiResponse<Void>> deleteComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal CustomUserDetails user
	) {
		communityService.deleteComment(commentId, user.getId(), user.isAdmin());
		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
