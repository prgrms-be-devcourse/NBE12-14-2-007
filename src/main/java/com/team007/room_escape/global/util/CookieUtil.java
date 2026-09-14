package com.team007.room_escape.global.util;

import com.team007.room_escape.global.security.AuthCookieProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
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

	public ResponseCookie createRefreshCookie(String value, Duration maxAge) {
		return base(value).maxAge(maxAge).build();
	}

	public ResponseCookie clearRefreshCookie() {
		return base("").maxAge(0).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
			.httpOnly(true)
			.secure(properties.secure())
			.sameSite(properties.sameSite())
			.path(properties.path());
	}
}
