package com.team007.room_escape.domain.post.controller;

import com.team007.room_escape.domain.post.infra.dto.PostCreateRequest;
import com.team007.room_escape.domain.post.infra.dto.PostCreateResponse;
import com.team007.room_escape.domain.post.infra.dto.PostDetailDto;
import com.team007.room_escape.domain.post.service.PostService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping("/festivals/{id}/posts")
	public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
			@PathVariable Long id,
			@Valid @RequestBody PostCreateRequest request,
			Authentication authentication
			) {
		CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();


		PostCreateResponse response = postService.createPost(id, request, user.getId());

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/posts/{id}")
	public ResponseEntity<ApiResponse<PostDetailDto>> getPostDetail(@PathVariable UUID id) {

		PostDetailDto postDetailDto = postService.findPostDetailById(id);

		return ResponseEntity.ok(ApiResponse.success(postDetailDto));
	}

}
