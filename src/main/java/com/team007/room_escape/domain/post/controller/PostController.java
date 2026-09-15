package com.team007.room_escape.domain.post.controller;

import com.team007.room_escape.domain.post.infra.dto.PostRequest;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.service.PostService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Post", description = "행사 후기 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@Operation(summary = "후기 작성", description = "특정 행사에 후기를 작성합니다.")
	@PostMapping("/festivals/{id}/posts")
	public ResponseEntity<ApiResponse<PostResponse.CreateResponse>> createPost(
			@PathVariable Long id,
			@Valid @RequestBody PostRequest.PostCreateRequest request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		PostResponse.CreateResponse response = postService.createPost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "후기 다건 조회", description = "후기 목록을 조회합니다.")
	@GetMapping("/festivals/{id}/posts")
	public ResponseEntity<ApiResponse<List<PostResponse.ListResponse>>> getPosts() {

		List<PostResponse.ListResponse> posts = postService.getPosts();

		return ResponseEntity.ok(ApiResponse.success(posts));
	}

	@Operation(summary = "후기 단건 조회", description = "후기 상세 정보를 조회합니다.")
	@GetMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostResponse.DetailResponse>> getPostDetail(@PathVariable UUID id) {

		PostResponse.DetailResponse postDetailDto = postService.findPostDetailById(id);

		return ResponseEntity.ok(ApiResponse.success(postDetailDto));
	}

	@Operation(summary = "후기 수정", description = "본인이 작성한 후기를 수정합니다.")
	@PatchMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostResponse.DetailResponse>> updatePost(
			@PathVariable UUID id,
			@Valid @RequestBody PostRequest.PostUpdateRequest request,
			@AuthenticationPrincipal CustomUserDetails user
	) {

		PostResponse.DetailResponse response = postService.updatePost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
