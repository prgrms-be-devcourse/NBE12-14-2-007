package com.team007.room_escape.global.security;

import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증되지 않은 요청이 보호 URL에 왔을 때 401을 만든다.
 * 시큐리티 필터 체인이라 @RestControllerAdvice가 직접 못 잡으므로
 * HandlerExceptionResolver로 넘겨 GlobalExceptionHandler가 TOKEN_MISSING을 내려준다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver resolver;

	public JwtAuthenticationEntryPoint(
		@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
	) {
		this.resolver = resolver;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) {
		resolver.resolveException(
			request, response, null,
			new BusinessException(AuthExceptionCode.TOKEN_MISSING)
		);
	}
}
