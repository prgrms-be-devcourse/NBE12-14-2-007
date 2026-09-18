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
	MEMBER_NOT_FOUND("MEMBER000", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
	/** 탈퇴하지 않은 계정이 같은 이메일을 이미 사용 중 */
	EMAIL_DUPLICATED("MEMBER001", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	/** 탈퇴하지 않은 계정이 같은 닉네임을 이미 사용 중 */
	NICKNAME_DUPLICATED("MEMBER002", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
	/** ROLE_WARNING 등급이 정보 수정 등 제한된 기능을 시도한 경우 */
	MEMBER_RESTRICTED("MEMBER003", HttpStatus.FORBIDDEN,
		"제재 중인 계정이라 정보를 수정할 수 없습니다. 관리자에게 문의해 주세요.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
