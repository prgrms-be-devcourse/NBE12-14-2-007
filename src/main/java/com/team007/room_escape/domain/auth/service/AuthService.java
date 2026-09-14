package com.team007.room_escape.domain.auth.service;

import com.team007.room_escape.domain.auth.dto.SignupRequest;
import com.team007.room_escape.domain.auth.dto.TokenPair;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.jwt.JwtProperties;
import com.team007.room_escape.global.jwt.JwtProvider;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public void signup(SignupRequest request) {
		if (memberRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
			throw new BusinessException(MemberExceptionCode.EMAIL_DUPLICATED);
		}
		if (memberRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())) {
			throw new BusinessException(MemberExceptionCode.NICKNAME_DUPLICATED);
		}

		memberRepository.save(
			Member.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.nickname(request.nickname())
				.phone(request.phone())
				.role(MemberRole.ROLE_USER)
				.build()
		);
	}

	@Transactional
	public TokenPair login(String email, String password) {
		Member member = memberRepository.findByEmailAndDeletedAtIsNull(email)
			.filter(found -> passwordEncoder.matches(password, found.getPassword()))
			.orElseThrow(() -> new BusinessException(AuthExceptionCode.INVALID_CREDENTIALS));
		return issueTokens(member);
	}

	@Transactional
	public String refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(AuthExceptionCode.TOKEN_MISSING);
		}

		Claims claims = jwtProvider.parseRefresh(refreshToken);
		UUID memberId = UUID.fromString(claims.getSubject());
		String nickname = claims.get("nickname", String.class);
		String role = claims.get("role", String.class);
		String profileImg = claims.get("profileImg", String.class);

		refreshTokenService.verify(memberId, refreshToken);

		return jwtProvider.createAccessToken(memberId, nickname, role, profileImg);
	}

	@Transactional
	public void logout(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		try {
			UUID memberId = UUID.fromString(jwtProvider.parseRefresh(refreshToken).getSubject());
			refreshTokenService.deleteByMemberId(memberId);
		} catch (RuntimeException ignored) {
			// 만료·위조된 쿠키면 DB에 지울 행이 없을 수 있다
		}
	}

	private TokenPair issueTokens(Member member) {
		String accessToken = jwtProvider.createAccessToken(
			member.getId(), member.getNickname(), member.authority(), member.getProfileImg());
		String refreshToken = jwtProvider.createRefreshToken(
			member.getId(), member.getNickname(), member.authority(), member.getProfileImg());

		refreshTokenService.save(
			member,
			refreshToken,
			LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenValiditySeconds())
		);
		return new TokenPair(accessToken, refreshToken);
	}
}
