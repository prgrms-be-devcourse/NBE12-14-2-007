package com.team007.room_escape.domain.auth.service;

import static com.team007.room_escape.global.util.StringUtil.emptyToNull;

import com.team007.room_escape.domain.auth.dto.AuthRequest.Signup;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public void signup(Signup request) {
		log.info("[가입] 요청 email={} nickname={} phone={} profileImg={}",
			request.email(), request.nickname(), request.phone(), request.profileImg());

		if (memberRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
			log.warn("[가입] 이메일 중복 email={}", request.email());
			throw new BusinessException(MemberExceptionCode.EMAIL_DUPLICATED);
		}
		if (memberRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())) {
			log.warn("[가입] 닉네임 중복 nickname={}", request.nickname());
			throw new BusinessException(MemberExceptionCode.NICKNAME_DUPLICATED);
		}

		Member saved = memberRepository.save(
			Member.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.nickname(request.nickname())
				// 빈 문자열이 그대로 저장되면 "번호 없음"과 구분이 안 되고 DB 제약에도 걸린다.
				.phone(emptyToNull(request.phone()))
				// 업로드 API가 돌려준 key를 그대로 저장한다. 공개 URL은 응답 시 조립한다.
				.profileImg(request.profileImg())
				.role(MemberRole.ROLE_UNVERIFIED)
				.build()
		);

		log.info("[가입] 완료 id={} email={} profileImg={}",
			saved.getId(), saved.getEmail(), saved.getProfileImg());
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
