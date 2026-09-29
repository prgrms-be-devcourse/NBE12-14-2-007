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
	 * 사용자가 올린 파일을 업로드하고 저장 key를 돌려준다.
	 *
	 * key에 업로드한 회원 ID를 넣어 두고, 저장·삭제할 때 본인 파일인지 이 값으로 확인한다.
	 * key는 공개 URL에 그대로 드러나므로 key를 안다고 해서 주인이라고 볼 수 없다.
	 *
	 * @param directory 버킷 안의 논리적 폴더 (예: posts, profiles)
	 * @param ownerId   업로드한 회원 ID
	 * @return 저장 key (예: posts/{회원ID}/0198....jpg)
	 */
	public String upload(MultipartFile file, String directory, UUID ownerId) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(StorageExceptionCode.EMPTY_FILE);
		}

		String extension = ALLOWED_TYPES.get(file.getContentType());
		if (extension == null) {
			throw new BusinessException(StorageExceptionCode.UNSUPPORTED_FILE_TYPE);
		}

		String key = "%s/%s/%s.%s".formatted(directory, ownerId, UUID.randomUUID(), extension);
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

	/**
	 * 이 회원이 업로드한 key인지. key 형식은 {폴더}/{회원ID}/{파일명}이다.
	 * 회원 ID가 들어가기 전에 올린 옛 key({폴더}/{파일명})는 주인을 알 수 없으므로 false다.
	 */
	public boolean isOwnedBy(String key, UUID memberId) {
		if (key == null || memberId == null) {
			return false;
		}
		String[] parts = key.split("/");
		return parts.length == 3 && parts[1].equals(memberId.toString());
	}

	/**
	 * 새로 저장하려는 이미지 key가 본인 것인지 확인한다.
	 * 비우는 경우(null, 빈 문자열)와 지금 값을 그대로 다시 보내는 경우는 통과시킨다.
	 */
	public void requireOwnedBy(String newKey, String currentKey, UUID memberId) {
		if (newKey == null || newKey.isBlank() || newKey.equals(currentKey)) {
			return;
		}
		if (!isOwnedBy(newKey, memberId)) {
			throw new BusinessException(StorageExceptionCode.IMAGE_NOT_OWNED);
		}
	}

	/** 본인이 올린 파일일 때만 지운다. 주인을 알 수 없는 옛 파일은 남겨둔다. */
	public void deleteOwnedBy(String key, UUID memberId) {
		if (isOwnedBy(key, memberId)) {
			delete(key);
		}
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
