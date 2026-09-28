package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 매니저 상세/신청 관련 에러 코드.
 * 신청중 중복, 이미 완료(매니저), 잘못된 상태 변경에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum ManagerExceptionCode implements ExceptionCode {

	/** 매니저 상세 행이 없음 */
	MANAGER_NOT_FOUND("MANAGER000", HttpStatus.NOT_FOUND, "매니저 정보를 찾을 수 없습니다."),
	/** 이미 신청중이라 다시 신청할 수 없음 */
	ALREADY_APPLYING("MANAGER001", HttpStatus.CONFLICT, "이미 매니저 신청 중입니다."),
	/** 이미 완료 상태(매니저)라 신청할 수 없음 */
	ALREADY_COMPLETED("MANAGER002", HttpStatus.CONFLICT, "이미 매니저입니다."),
	/** 현재 상태에서 할 수 없는 승인/해제 */
	INVALID_STATUS("MANAGER003", HttpStatus.BAD_REQUEST, "현재 상태에서는 처리할 수 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
