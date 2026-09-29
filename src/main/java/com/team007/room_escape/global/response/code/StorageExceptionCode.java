package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageExceptionCode implements ExceptionCode {

	EMPTY_FILE("STOR000", HttpStatus.BAD_REQUEST, "업로드할 파일이 비어 있습니다."),
	UNSUPPORTED_FILE_TYPE("STOR001", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
	UPLOAD_FAILED("STOR002", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
	/** 남이 올린 이미지 key를 자기 데이터에 넣으면, 나중에 교체할 때 남의 파일이 지워진다 */
	IMAGE_NOT_OWNED("STOR003", HttpStatus.FORBIDDEN, "본인이 업로드한 이미지만 사용할 수 있습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
