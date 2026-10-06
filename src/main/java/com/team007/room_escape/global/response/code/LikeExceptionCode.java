package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LikeExceptionCode implements ExceptionCode {

	/** 이미 좋아요를 등록한 경우 */
	LIKE_ALREADY_EXISTS("LIKE000", HttpStatus.CONFLICT, "이미 좋아요를 등록했습니다."),
	/** 좋아요가 존재하지 않는 경우 */
	LIKE_NOT_FOUND("LIKE001", HttpStatus.NOT_FOUND, "등록된 좋아요가 없습니다."),
	/** 본인이 제보한 행사에 좋아요를 등록하려는 경우 */
	SELF_FESTIVAL_LIKE_NOT_ALLOWED("LIKE002", HttpStatus.FORBIDDEN,
		"본인이 제보한 행사에는 좋아요를 등록할 수 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
