package com.team007.room_escape.global.util;

import com.team007.room_escape.global.security.AuthCookieProperties;
import com.team007.room_escape.global.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh Token HttpOnly 쿠키 생성·삭제.
 * SameSite를 위해 jakarta Cookie 대신 Spring ResponseCookie를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

	public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

	private final AuthCookieProperties properties;
	private final JwtProperties jwtProperties;

	public void addRefreshCookie(HttpServletResponse response, String token) {
		ResponseCookie cookie = base(token)
			.maxAge(jwtProperties.refreshTokenValiditySeconds())
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie cookie = base("").maxAge(0).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
			.httpOnly(true)
			.secure(properties.secure())
			.sameSite(properties.sameSite())
			.path(properties.path());
	}
}
