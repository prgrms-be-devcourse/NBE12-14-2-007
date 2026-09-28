package com.team007.room_escape.domain.auth.controller;

import com.team007.room_escape.domain.auth.dto.AuthRequest.Login;
import com.team007.room_escape.domain.auth.dto.AuthRequest.Signup;
import com.team007.room_escape.domain.auth.dto.TokenPair;
import com.team007.room_escape.domain.auth.dto.TokenResponse;
import com.team007.room_escape.domain.auth.service.AuthService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인 및 토큰 관리")
public class AuthController {

	private final AuthService authService;
	private final CookieUtil cookieUtil;

	@Operation(summary = "회원가입")
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody Signup request) {
		authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.noContentSuccess());
	}

	@Operation(summary = "로그인")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(
		@Valid @RequestBody Login request,
		HttpServletResponse response
	) {
		TokenPair pair = authService.login(request.email(), request.password());
		cookieUtil.addRefreshCookie(response, pair.refreshToken());
		return ResponseEntity.ok(
			ApiResponse.success(new TokenResponse(pair.accessToken(), "Bearer"))
		);
	}

	@Operation(summary = "Access Token 재발급")
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
		@CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
	) {
		String accessToken = authService.refresh(refreshToken);
		return ResponseEntity.ok(
			ApiResponse.success(new TokenResponse(accessToken, "Bearer"))
		);
	}

	@Operation(summary = "로그아웃")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
		HttpServletResponse response
	) {
		authService.logout(refreshToken);
		cookieUtil.clearRefreshCookie(response);
		return ResponseEntity.noContent().build();
	}
}
