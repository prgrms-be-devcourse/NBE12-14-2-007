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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Cloudflare R2 파일 저장소. DB에는 URL이 아니라 key만 저장해서 도메인이 바뀌어도 데이터를 안 건드린다. */
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

	/** 업로드 후 key를 돌려준다. key는 공개되므로 주인 확인용으로 key에 회원 ID를 넣는다. (예: posts/{회원ID}/0198....jpg) */
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
					// key가 UUID라 내용이 안 바뀌어서 1년 캐시해도 안전하다.
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

	/** 서버가 만든 바이트 배열을 업로드한다. 배치처럼 MultipartFile이 없을 때 쓰고, contentType은 직접 넘긴다. */
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

	/** 이 회원이 올린 key인지. 형식은 {폴더}/{회원ID}/{파일명}이고, 회원 ID가 없는 옛 key는 false다. */
	public boolean isOwnedBy(String key, UUID memberId) {
		if (key == null || memberId == null) {
			return false;
		}
		String[] parts = key.split("/");
		return parts.length == 3 && parts[1].equals(memberId.toString());
	}

	/** 새로 저장할 key가 본인 것인지 확인한다. 비우거나 지금 값 그대로면 통과시킨다. */
	public void requireOwnedBy(String newKey, String currentKey, UUID memberId) {
		if (newKey == null || newKey.isBlank() || newKey.equals(currentKey)) {
			return;
		}
		if (!isOwnedBy(newKey, memberId)) {
			throw new BusinessException(StorageExceptionCode.IMAGE_NOT_OWNED);
		}
	}

	/** 본인 파일만 지운다. 롤백 시 이미지가 깨지지 않게 트랜잭션 커밋 후에 지운다. */
	public void deleteOwnedBy(String key, UUID memberId) {
		if (!isOwnedBy(key, memberId)) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			delete(key);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				delete(key);
			}
		});
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
