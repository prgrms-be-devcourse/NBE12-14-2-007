package com.team007.room_escape.global.storage;

import com.team007.room_escape.global.config.R2Properties;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.StorageExceptionCode;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cloudflare R2 파일 저장소.
 * DB에는 공개 URL이 아니라 key만 저장하고, 내려줄 때 ImageUrlResolver로 조합한다.
 * 도메인이 바뀌어도 데이터를 손대지 않기 위함이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

	/** 허용 Content-Type과 저장 시 사용할 확장자 */
	private static final Map<String, String> ALLOWED_TYPES = Map.of(
		"image/jpeg", "jpg",
		"image/png", "png",
		"image/webp", "webp"
	);

	private final S3Client s3Client;
	private final R2Properties properties;

	/**
	 * 파일을 업로드하고 저장 key를 돌려준다.
	 *
	 * @param directory 버킷 안의 논리적 폴더 (예: posts, profiles)
	 * @return 저장 key (예: posts/0198....jpg)
	 */
	public String upload(MultipartFile file, String directory) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(StorageExceptionCode.EMPTY_FILE);
		}

		String extension = ALLOWED_TYPES.get(file.getContentType());
		if (extension == null) {
			throw new BusinessException(StorageExceptionCode.UNSUPPORTED_FILE_TYPE);
		}

		String key = "%s/%s.%s".formatted(directory, UUID.randomUUID(), extension);
		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(properties.bucket())
					.key(key)
					.contentType(file.getContentType())
					// key가 UUID라 이 URL의 내용은 평생 안 바뀐다 -> 1년 캐시해도 안전함 (CDN·브라우저 둘 다 적용됨)
					.cacheControl("public, max-age=31536000, immutable")
					.build(),
				RequestBody.fromInputStream(file.getInputStream(), file.getSize())
			);
		} catch (IOException | SdkException e) {
			log.error("R2 업로드 실패 key={}", key, e);
			throw new BusinessException(StorageExceptionCode.UPLOAD_FAILED);
		}
		return key;
	}

	/**
	 * 서버가 직접 만든 바이트 배열을 업로드한다 (외부 URL 다운로드·리사이즈 결과 등).
	 * MultipartFile이 없는 경우(HTTP 요청이 아닌 배치 처리 등)에 사용한다.
	 *
	 * @param content     업로드할 바이트 데이터 (예: 리사이즈된 이미지)
	 * @param contentType MIME 타입. MultipartFile처럼 자동으로 안 들어오므로 호출부가 직접 넘겨야 한다
	 * @param directory   버킷 안의 논리적 폴더 (예: festivals)
	 * @return 저장 key
	 */
	public String upload(byte[] content, String contentType, String directory) {
		String extension = ALLOWED_TYPES.get(contentType);
		if (extension == null) {
			throw new BusinessException(StorageExceptionCode.UNSUPPORTED_FILE_TYPE);
		}

		String key = "%s/%s.%s".formatted(directory, UUID.randomUUID(), extension);
		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(properties.bucket())
					.key(key)
					.contentType(contentType)
					.cacheControl("public, max-age=31536000, immutable")
					.build(),
				RequestBody.fromBytes(content)
			);
		} catch (SdkException e) {
			log.error("R2 업로드 실패 key={}", key, e);
			throw new BusinessException(StorageExceptionCode.UPLOAD_FAILED);
		}
		return key;
	}

	/** 삭제 실패는 로그만 남긴다. 파일이 남는 것보다 비즈니스 흐름이 끊기는 게 더 나쁘다. */
	public void delete(String key) {
		if (key == null || key.isBlank()) {
			return;
		}
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key)
				.build());
		} catch (SdkException e) {
			log.warn("R2 삭제 실패 key={}", key, e);
		}
	}

}
