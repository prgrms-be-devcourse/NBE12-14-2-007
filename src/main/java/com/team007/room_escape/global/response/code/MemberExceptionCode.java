package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원 도메인 에러 코드.
 * 조회 실패 등 회원 관련 비즈니스 예외에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum MemberExceptionCode implements ExceptionCode {

	/** 해당 id/이메일 회원이 없거나 이미 소프트 삭제된 경우 */
	MEMBER_NOT_FOUND("MEMBER000", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
