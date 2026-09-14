package com.team007.room_escape.domain.auth.service;

import com.team007.room_escape.domain.auth.infra.entity.RefreshToken;
import com.team007.room_escape.domain.auth.infra.repository.RefreshTokenRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;

	@Transactional
	public void save(Member member, String rawToken, LocalDateTime expiresAt) {
		String hash = sha256(rawToken);
		refreshTokenRepository.findByMember_Id(member.getId()).ifPresentOrElse(
			existing -> existing.rotate(hash, expiresAt),
			() -> refreshTokenRepository.save(new RefreshToken(member, hash, expiresAt))
		);
	}

	@Transactional(readOnly = true)
	public void verify(UUID memberId, String rawToken) {
		RefreshToken stored = refreshTokenRepository.findByMember_Id(memberId)
			.orElseThrow(() -> new BusinessException(AuthExceptionCode.TOKEN_INVALID));

		if (stored.isExpired()) {
			throw new BusinessException(AuthExceptionCode.TOKEN_EXPIRED);
		}
		if (!stored.matches(sha256(rawToken))) {
			throw new BusinessException(AuthExceptionCode.TOKEN_INVALID);
		}
	}

	@Transactional
	public void deleteByMemberId(UUID memberId) {
		refreshTokenRepository.deleteByMember_Id(memberId);
	}

	private String sha256(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 미지원", e);
		}
	}
}
