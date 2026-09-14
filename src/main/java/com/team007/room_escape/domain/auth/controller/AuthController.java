package com.team007.room_escape.domain.auth.controller;

import com.team007.room_escape.domain.auth.dto.LoginRequest;
import com.team007.room_escape.domain.auth.dto.TokenPair;
import com.team007.room_escape.domain.auth.dto.TokenResponse;
import com.team007.room_escape.domain.auth.service.AuthService;
import com.team007.room_escape.global.jwt.JwtProperties;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CookieUtil cookieUtil;
	private final JwtProperties jwtProperties;

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse response
	) {
		TokenPair pair = authService.login(request.email(), request.password());
		addRefreshCookie(response, pair.refreshToken());
		return ApiResponse.success(new TokenResponse(pair.accessToken(), "Bearer"));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(
		@CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletResponse response
	) {
		TokenPair pair = authService.refresh(refreshToken);
		if (pair.refreshRotated()) {
			addRefreshCookie(response, pair.refreshToken());
		}
		return ApiResponse.success(new TokenResponse(pair.accessToken(), "Bearer"));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		@CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletResponse response
	) {
		authService.logout(refreshToken);
		response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.clearRefreshCookie().toString());
		return ApiResponse.noContentSuccess();
	}

	private void addRefreshCookie(HttpServletResponse response, String token) {
		response.addHeader(
			HttpHeaders.SET_COOKIE,
			cookieUtil.createRefreshCookie(
				token,
				Duration.ofSeconds(jwtProperties.refreshTokenValiditySeconds())
			).toString()
		);
	}
}
