package com.team007.room_escape.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team007.room_escape.global.config.R2Properties;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.StorageExceptionCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@ExtendWith(MockitoExtension.class)
class R2StorageServiceOwnershipTest {

	@Mock S3Client s3Client;
	@Mock R2Properties properties;

	@InjectMocks R2StorageService storageService;

	private final UUID me = UUID.randomUUID();
	private final UUID other = UUID.randomUUID();

	@Test
	@DisplayName("key의 두 번째 자리가 회원 ID와 같을 때만 본인 파일로 본다")
	void isOwnedBy() {
		assertThat(storageService.isOwnedBy("profiles/" + me + "/a.png", me)).isTrue();
		assertThat(storageService.isOwnedBy("profiles/" + other + "/a.png", me)).isFalse();
		assertThat(storageService.isOwnedBy("profiles/a.png", me)).isFalse();
		assertThat(storageService.isOwnedBy("profiles/guest/a.png", me)).isFalse();
		assertThat(storageService.isOwnedBy(null, me)).isFalse();
	}

	@Test
	@DisplayName("남이 올린 이미지 key로 바꾸려 하면 거절한다")
	void rejectOthersKey() {
		assertThatThrownBy(() -> storageService.requireOwnedBy("inquiries/" + other + "/a.png", null, me))
			.isInstanceOfSatisfying(BusinessException.class, e ->
				assertThat(e.getExceptionCode()).isEqualTo(StorageExceptionCode.IMAGE_NOT_OWNED));
	}

	@Test
	@DisplayName("비우거나 지금 값을 그대로 보내면 주인 확인 없이 통과한다")
	void allowClearOrUnchanged() {
		String legacy = "profiles/legacy.png";

		assertThatCode(() -> storageService.requireOwnedBy(null, legacy, me)).doesNotThrowAnyException();
		assertThatCode(() -> storageService.requireOwnedBy("", legacy, me)).doesNotThrowAnyException();
		assertThatCode(() -> storageService.requireOwnedBy(legacy, legacy, me)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("남의 파일이나 주인을 알 수 없는 옛 파일은 지우지 않는다")
	void deleteOnlyOwnFiles() {
		storageService.deleteOwnedBy("profiles/" + other + "/a.png", me);
		storageService.deleteOwnedBy("profiles/legacy.png", me);

		verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));

		storageService.deleteOwnedBy("profiles/" + me + "/a.png", me);

		verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
	}
}
