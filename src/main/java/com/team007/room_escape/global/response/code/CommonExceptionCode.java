package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인에 묶이지 않는 공통 에러 코드.
 * 요청 검증 실패, 처리하지 못한 서버 오류에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonExceptionCode implements ExceptionCode {

	/** @Valid 실패 등 요청 값/형식이 잘못된 경우 (400) */
	INVALID_INPUT("COMMON_INVALID_INPUT", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	/** spring.servlet.multipart.max-file-size 를 넘는 파일을 올린 경우 (413) */
	FILE_TOO_LARGE("COMMON_FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE,
		"파일 크기가 너무 큽니다. 5MB 이하로 올려 주세요."),
	/** 예상하지 못한 서버 예외. 원문 메시지는 클라이언트에 노출하지 않는다 (500) */
	INTERNAL_ERROR("COMMON_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
