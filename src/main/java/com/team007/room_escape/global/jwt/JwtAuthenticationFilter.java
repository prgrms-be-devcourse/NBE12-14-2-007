package com.team007.room_escape.global.jwt;

import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 요청마다 Authorization Bearer 토큰을 읽어 SecurityContext에 인증을 넣는다.
 * 토큰 없음은 통과(공개 URL은 그대로, 보호 URL은 EntryPoint가 401),
 * 만료/위조는 GlobalExceptionHandler와 같은 ApiResponse로 거절한다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;
	private final HandlerExceptionResolver resolver;

	public JwtAuthenticationFilter(
		JwtProvider jwtProvider,
		@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
	) {
		this.jwtProvider = jwtProvider;
		this.resolver = resolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return path.startsWith("/api/v1/auth/")
			|| path.startsWith("/swagger-ui")
			|| path.startsWith("/v3/api-docs");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Claims claims = jwtProvider.parse(token);
			CustomUserDetails principal = new CustomUserDetails(
				UUID.fromString(claims.getSubject()),
				claims.get("nickname", String.class),
				claims.get("role", String.class),
				claims.get("profileImg", String.class),
				null
			);

			UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			reject(request, response, AuthExceptionCode.TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			reject(request, response, AuthExceptionCode.TOKEN_INVALID);
		}
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, AuthExceptionCode code) {
		SecurityContextHolder.clearContext();
		resolver.resolveException(request, response, null, new BusinessException(code));
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HEADER);
		if (header == null || !header.startsWith(PREFIX)) {
			return null;
		}
		String token = header.substring(PREFIX.length()).trim();
		if (token.isEmpty() || "null".equals(token) || "undefined".equals(token)) {
			return null;
		}
		return token;
	}
}
