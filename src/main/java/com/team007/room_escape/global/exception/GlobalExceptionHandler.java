package com.team007.room_escape.global.exception;

import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러·필터에서 발생한 예외를 ApiResponse 형식으로 통일해 응답한다.
 * 필터 단계 예외도 HandlerExceptionResolver를 통해 여기로 들어온다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 서비스에서 의도적으로 던진 비즈니스 예외 (AUTH/MEMBER 등 ExceptionCode 전부) */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
		ExceptionCode ec = e.getExceptionCode();
		logByStatus(ec, e);
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(e.getMessage(), ec.getCode()));
	}

	/** @Valid 요청 본문 검증 실패 (필드 제약 위반) */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
			.collect(Collectors.joining(", "));
		CommonExceptionCode ec = CommonExceptionCode.INVALID_INPUT;
		log.warn("[{}] {}", ec.getCode(), message);
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(message, ec.getCode()));
	}

	/** Refresh 등에서 JWT 만료. 필터의 만료 토큰은 BusinessException으로 먼저 처리된다 */
	@ExceptionHandler(ExpiredJwtException.class)
	public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException e) {
		AuthExceptionCode ec = AuthExceptionCode.TOKEN_EXPIRED;
		log.warn("[{}] {}", ec.getCode(), e.getMessage());
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec.getMessage(), ec.getCode()));
	}

	/** JWT 서명 불일치·형식 오류 등 만료 이외의 토큰 문제 */
	@ExceptionHandler(JwtException.class)
	public ResponseEntity<ApiResponse<Void>> handleJwt(JwtException e) {
		AuthExceptionCode ec = AuthExceptionCode.TOKEN_INVALID;
		log.warn("[{}] {}", ec.getCode(), e.getMessage());
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec.getMessage(), ec.getCode()));
	}

	/** 시큐리티 인증 실패. 주로 @PreAuthorize 등 컨트롤러 구간. 필터 401은 EntryPoint가 처리 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
		AuthExceptionCode ec = AuthExceptionCode.AUTHENTICATION_FAILED;
		log.warn("[{}] AuthenticationException: {}", ec.getCode(), e.getMessage());
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec.getMessage(), ec.getCode()));
	}

	/** 인증은 됐으나 권한 부족. 주로 @PreAuthorize(hasRole) 실패. 필터 403은 AccessDeniedHandler */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
		AuthExceptionCode ec = AuthExceptionCode.ACCESS_DENIED;
		log.warn("[{}] AccessDeniedException: {}", ec.getCode(), e.getMessage());
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec.getMessage(), ec.getCode()));
	}

	/** 위에서 못 잡은 모든 예외. 원문 메시지는 노출하지 않는다 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
		CommonExceptionCode ec = CommonExceptionCode.INTERNAL_ERROR;
		log.error("[UNHANDLED] 처리되지 않은 예외", e);
		return ResponseEntity.status(ec.getStatus())
			.body(ApiResponse.error(ec.getMessage(), ec.getCode()));
	}

	/** 5xx는 스택트레이스까지, 4xx는 메시지만 남긴다 */
	private void logByStatus(ExceptionCode ec, Exception e) {
		if (ec.getStatus().is5xxServerError()) {
			log.error("[{}] {}", ec.getCode(), e.getMessage(), e);
		} else {
			log.warn("[{}] {}", ec.getCode(), e.getMessage());
		}
	}
}
