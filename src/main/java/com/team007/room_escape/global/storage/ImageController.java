package com.team007.room_escape.global.storage;

import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.Getter;
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

	/**
	 * 업로드 대상 구분. 버킷 안에서 어느 폴더에 저장할지를 정한다.
	 * 클라이언트가 임의 경로를 넘기지 못하도록 enum으로 고정한다.
	 */
	@Getter
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public enum ImageType {

		POST("posts"),
		PROFILE("profiles"),
		INQUIRY("inquiries"),
		FESTIVAL("festivals");

		private final String directory;
	}

	/**
	 * 업로드 결과.
	 *
	 * @param key DB에 저장할 값. URL이 아니라 이 key를 저장한다
	 * @param url 화면에서 바로 쓸 수 있는 공개 URL
	 */
	public record UploadInfo(String key, String url) {
	}

	private final R2StorageService r2StorageService;
	private final ImageUrlResolver imageUrlResolver;

	@Operation(
		summary = "이미지 업로드",
		description = "이미지를 업로드하고 저장 key와 공개 URL을 돌려준다. "
			+ "DB에는 URL이 아니라 key를 저장한다. jpeg/png/webp만 허용하며 최대 5MB."
	)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<UploadInfo>> upload(
		// 이름을 생략하면 컴파일 옵션(-parameters)에 의존하게 된다. 명시해두면 어떤 빌드에서도 안전하다.
		@RequestParam("type") ImageType type,
		@RequestPart("file") MultipartFile file
	) {
		String key = r2StorageService.upload(file, type.getDirectory());

		return ResponseEntity.ok(ApiResponse.success(
			new UploadInfo(key, imageUrlResolver.resolve(key))));
	}
}
