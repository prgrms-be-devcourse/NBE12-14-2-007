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
    FESTIVAL_NOT_FOUND("FESTIVAL001", HttpStatus.NOT_FOUND, "존재하지 않는 행사입니다."),

    /** 공공 API 호출 자체가 실패 (네트워크 오류, 타임아웃, 4xx/5xx 응답 등) */
    PUBLIC_API_CALL_FAILED("FESTIVAL001", HttpStatus.SERVICE_UNAVAILABLE, "공공 API 호출에 실패했습니다."),

    /** 공공 API 응답 파싱 실패 */
    PUBLIC_API_PARSE_FAILED("FESTIVAL001", HttpStatus.INTERNAL_SERVER_ERROR, "공공 API 응답 파싱에 실패했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}

