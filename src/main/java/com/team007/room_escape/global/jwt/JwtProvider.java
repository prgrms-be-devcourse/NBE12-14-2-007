package com.team007.room_escape.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 생성·검증.
 * Access와 Refresh는 서로 다른 키로 서명하고, sub에 회원 UUID, 클레임에 nickname/role/profileImg를 넣는다.
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

	private final JwtProperties properties;

	private SecretKey accessKey;
	private SecretKey refreshKey;

	@PostConstruct
	void init() {
		this.accessKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.refreshKey = Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(UUID memberId, String nickname, String role, String profileImg) {
		return build(memberId, nickname, role, profileImg, properties.accessTokenValiditySeconds(), accessKey);
	}

	public String createRefreshToken(UUID memberId, String nickname, String role, String profileImg) {
		return build(memberId, nickname, role, profileImg, properties.refreshTokenValiditySeconds(), refreshKey);
	}

	private String build(
		UUID memberId,
		String nickname,
		String role,
		String profileImg,
		long validitySeconds,
		SecretKey key
	) {
		Instant now = Instant.now();
		var builder = Jwts.builder()
			.issuer(properties.issuer())
			.subject(memberId.toString())
			.claim("nickname", nickname)
			.claim("role", role)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(validitySeconds)))
			.signWith(key);
		if (profileImg != null) {
			builder.claim("profileImg", profileImg);
		}
		return builder.compact();
	}

	public Claims parse(String token) {
		return parseWith(token, accessKey);
	}

	public Claims parseRefresh(String token) {
		return parseWith(token, refreshKey);
	}

	private Claims parseWith(String token, SecretKey key) {
		return Jwts.parser()
			.verifyWith(key)
			.requireIssuer(properties.issuer())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}
}
