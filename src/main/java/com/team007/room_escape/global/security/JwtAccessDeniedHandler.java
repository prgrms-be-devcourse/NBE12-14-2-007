package com.team007.room_escape.global.security;

import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증은 됐지만 권한이 부족할 때 403을 만든다.
 * 필터 체인의 AccessDenied를 HandlerExceptionResolver로 넘겨
 * GlobalExceptionHandler가 ACCESS_DENIED를 내려준다.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public JwtAccessDeniedHandler(
		@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
	) {
		this.resolver = resolver;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) {
		resolver.resolveException(
			request, response, null,
			new BusinessException(AuthExceptionCode.ACCESS_DENIED)
		);
	}
}
