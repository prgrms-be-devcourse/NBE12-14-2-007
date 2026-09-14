package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 인증·인가·토큰 관련 에러 코드.
 * 로그인 실패, 권한 부족, JWT 누락/위조/만료에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthExceptionCode implements ExceptionCode {

	/** 인증 실패. 원인을 특정하지 못할 때 (시큐리티 AuthenticationException 등) */
	AUTHENTICATION_FAILED("AUTH000", HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
	/** 이메일 없음 또는 비밀번호 불일치. 존재 여부는 노출하지 않는다 */
	INVALID_CREDENTIALS("AUTH001", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	/** 잠긴 계정으로 로그인 시도 */
	ACCOUNT_LOCKED("AUTH002", HttpStatus.UNAUTHORIZED, "계정이 잠겨 있습니다. 관리자에게 문의하세요."),
	/** 비활성화(정지)된 계정 */
	ACCOUNT_DISABLED("AUTH003", HttpStatus.UNAUTHORIZED, "비활성화된 계정입니다."),
	/** 계정 또는 자격 증명이 만료됨 */
	ACCOUNT_EXPIRED("AUTH005", HttpStatus.UNAUTHORIZED, "만료된 계정입니다."),
	/** 2차 인증(MFA)이 더 필요함 */
	MULTI_FACTOR_REQUIRED("AUTH006", HttpStatus.UNAUTHORIZED, "추가 인증이 필요합니다."),

	/** 로그인은 됐으나 권한 부족 (예: USER가 관리자 API 호출, 403) */
	ACCESS_DENIED("AUTH100", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	/** 세션이 만료되어 다시 로그인이 필요함 */
	SESSION_EXPIRED("AUTH101", HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요."),

	/** Authorization 헤더/쿠키에 토큰이 없음 */
	TOKEN_MISSING("AUTH200", HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다."),
	/** 서명 불일치, 형식 오류, 로그아웃으로 폐기된 Refresh 등 */
	TOKEN_INVALID("AUTH201", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	/** Access 또는 Refresh 만료 */
	TOKEN_EXPIRED("AUTH202", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
