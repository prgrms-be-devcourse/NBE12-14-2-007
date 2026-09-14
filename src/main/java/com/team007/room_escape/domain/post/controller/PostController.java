package com.team007.room_escape.domain.post.controller;

import com.team007.room_escape.domain.post.infra.dto.PostDetailDto;
import com.team007.room_escape.domain.post.service.PostService;
import com.team007.room_escape.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PostDetailDto>> getPostDetail(@PathVariable UUID id) {

		PostDetailDto postDetailDto = postService.findPostDetailById(id);

		return ResponseEntity.ok(ApiResponse.success(postDetailDto));
	}

}
