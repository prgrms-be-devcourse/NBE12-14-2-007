package com.team007.room_escape.domain.post.controller;

import com.team007.room_escape.domain.post.infra.dto.PostRequest;
import com.team007.room_escape.domain.post.infra.dto.PostResponse;
import com.team007.room_escape.domain.post.service.PostService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping("/festivals/{id}/posts")
	public ResponseEntity<ApiResponse<PostResponse.CreateResponse>> createPost(
			@PathVariable Long id,
			@Valid @RequestBody PostRequest.PostCreateRequest request,
			@AuthenticationPrincipal CustomUserDetails user
			) {

		PostResponse.CreateResponse response = postService.createPost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostResponse.DetailResponse>> getPostDetail(@PathVariable UUID id) {

		PostResponse.DetailResponse postDetailDto = postService.findPostDetailById(id);

		return ResponseEntity.ok(ApiResponse.success(postDetailDto));
	}

}
