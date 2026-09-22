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
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
			@Valid @RequestBody PostRequest request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		PostResponse.CreateResponse response = postService.createPost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "행사별 후기 다건 조회", description = "행사별 후기 목록을 조회합니다.")
	@GetMapping("/festivals/{id}/posts")
	public ResponseEntity<ApiResponse<Page<PostResponse.ListResponse>>> getPostsByFestival(
			@PathVariable Long id,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
			Pageable page) {

		Page<PostResponse.ListResponse> posts = postService.getPosts(id, page);

		return ResponseEntity.ok(ApiResponse.success(posts));
	}

	@Operation(summary = "후기 단건 조회", description = "후기 상세 정보를 조회합니다.")
	@GetMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostResponse.DetailResponse>> getPostDetail(@PathVariable UUID id) {

		PostResponse.DetailResponse postDetailDto = postService.getPostDetail(id);

		return ResponseEntity.ok(ApiResponse.success(postDetailDto));
	}

	@Operation(summary = "후기 수정", description = "본인이 작성한 후기를 수정합니다.")
	@PatchMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostResponse.DetailResponse>> updatePost(
			@PathVariable UUID id,
			@Valid @RequestBody PostRequest request,
			@AuthenticationPrincipal CustomUserDetails user
	) {

		PostResponse.DetailResponse response = postService.updatePost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "후기 삭제", description = "작성자 본인 또는 관리자가 후기를 삭제합니다")
	@DeleteMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<Void>> deletePost(
			@PathVariable UUID id,
			@AuthenticationPrincipal CustomUserDetails user
	) {
		postService.deletePost(id, user.getId(), user.isAdmin());

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
