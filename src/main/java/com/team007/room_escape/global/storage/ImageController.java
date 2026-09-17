package com.team007.room_escape.global.storage;

import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 공통 API.
 * 프론트는 업로드로 key를 먼저 받아두고, 실제 등록/수정 요청에 그 key를 담아 보낸다.
 */
@Tag(name = "Image", description = "이미지 업로드 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

	private final R2StorageService r2StorageService;

	@Operation(
		summary = "이미지 업로드",
		description = "이미지를 업로드하고 저장 key와 공개 URL을 돌려준다. "
			+ "DB에는 URL이 아니라 key를 저장한다. jpeg/png/webp만 허용하며 최대 5MB."
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
		@RequestParam ImageType type,
		@RequestPart("file") MultipartFile file
	) {
		String key = r2StorageService.upload(file, type.getDirectory());

		return ResponseEntity.ok(ApiResponse.success(
			ImageUploadResponse.of(key, r2StorageService.toPublicUrl(key))));
	}
}
