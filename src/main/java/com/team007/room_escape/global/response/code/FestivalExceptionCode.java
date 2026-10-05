package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 행사(festival) 도메인 에러 코드 (API 연동 관련 실패에 사용)
 */
@Getter
@RequiredArgsConstructor
public enum FestivalExceptionCode implements ExceptionCode {
	/** festival 테이블에서 행사 자체를 찾을 수 없을 때 */
	FESTIVAL_NOT_FOUND("FESTIVAL001", HttpStatus.NOT_FOUND, "존재하지 않는 행사입니다."),

	/** 공공 API 호출 자체가 실패 (네트워크 오류, 타임아웃, 4xx/5xx 응답 등) */
	PUBLIC_API_CALL_FAILED("FESTIVAL002", HttpStatus.SERVICE_UNAVAILABLE, "공공 API 호출에 실패했습니다."),

	/** 공공 API 응답 파싱 실패 */
	PUBLIC_API_PARSE_FAILED("FESTIVAL003", HttpStatus.INTERNAL_SERVER_ERROR, "공공 API 응답 파싱에 실패했습니다."),

	/** 행사 제보가 존재하지 않거나 로그인한 회원의 제보가 아닐 때 */
	FESTIVAL_SUBMISSION_NOT_FOUND("FESTIVAL004", HttpStatus.NOT_FOUND, "요청하신 행사 제보를 찾을 수 없습니다."),

	/** 이미 다른 동기화가 실행 중일 때 (버튼 연타 등) */
	SYNC_ALREADY_RUNNING("FESTIVAL005", HttpStatus.CONFLICT, "이미 공공 행사 동기화가 진행 중입니다."),

	/** 동일한 일정·지역·링크의 행사가 존재할 때 */
	DUPLICATE_FESTIVAL("FESTIVAL006", HttpStatus.CONFLICT, "이미 등록된 행사입니다."),

	/** 삭제되지 않은 행사에 복구를 요청한 경우 */
	FESTIVAL_NOT_DELETED("FESTIVAL007", HttpStatus.CONFLICT, "삭제되지 않은 행사입니다."),

	/** 회원이 직접 지운 제보를 관리자가 임의로 되살리지 않도록 막는다 */
	FESTIVAL_RESTORE_FORBIDDEN("FESTIVAL008", HttpStatus.FORBIDDEN,
		"회원이 제보한 행사는 복구할 수 없습니다."),

	/** 본인이 제보한 행사를 직접 정확하다고 평가하는 경우 */
	SELF_ACCURACY_VOTE_NOT_ALLOWED("FESTIVAL009", HttpStatus.FORBIDDEN,
		"본인이 제보한 행사의 정확도는 평가할 수 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}

