package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostExceptionCode implements ExceptionCode {
	/** 요청한 후기가 존재하지 않는 경우 */
	POST_NOT_FOUND("POST000", HttpStatus.NOT_FOUND, "존재하지 않는 후기입니다."),
	/** 후기 작성자가 아닌 사용자가 수정 또는 삭제를 시도한 경우 */
	POST_FORBIDDEN("POST001", HttpStatus.FORBIDDEN, "해당 후기를 수정하거나 삭제 할 권한이 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
