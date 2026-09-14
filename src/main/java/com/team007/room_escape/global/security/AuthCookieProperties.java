package com.team007.room_escape.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 쿠키 속성 (app.cookie.*).
 * 로컬 http는 secure=false, SameSite=Lax. 프론트·백 도메인이 다르고 https면 None+secure=true.
 */
@ConfigurationProperties(prefix = "app.cookie")
public record AuthCookieProperties(
	boolean secure,
	String sameSite,
	String path
) {
}
