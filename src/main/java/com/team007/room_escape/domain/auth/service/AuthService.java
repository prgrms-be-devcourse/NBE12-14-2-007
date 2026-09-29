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

	/** 비회원은 이미지를 올릴 수 없어서 프로필 사진은 가입 후 마이페이지에서 등록한다. */
	@Transactional
	public void signup(Signup request) {
		log.info("[가입] 요청 email={} nickname={} phone={}",
			request.email(), request.nickname(), request.phone());

		// 탈퇴 회원까지 본다. 탈퇴 직후 같은 이메일로 다시 가입하는 것을 막기 위함이다.
		if (memberRepository.existsByEmail(request.email())) {
			// 쓰는 중인지 탈퇴한 것인지 구분해서 알려준다. 둘 다 "이 이메일이 존재한다"는
			// 사실은 같으므로 노출되는 정보의 양은 다르지 않고, 안내만 정확해진다.
			boolean inUse = memberRepository.existsByEmailAndDeletedAtIsNull(request.email());
			log.warn("[가입] 이메일 중복 email={} inUse={}", request.email(), inUse);
			throw new BusinessException(inUse
				? MemberExceptionCode.EMAIL_DUPLICATED
				: MemberExceptionCode.EMAIL_WITHDRAWN);
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
				.role(MemberRole.ROLE_UNVERIFIED)
				.build()
		);

		log.info("[가입] 완료 id={} email={}", saved.getId(), saved.getEmail());
	}

	@Transactional
	public TokenPair login(String email, String password) {
		Member member = memberRepository.findByEmailAndDeletedAtIsNull(email)
			.filter(found -> passwordEncoder.matches(password, found.getPassword()))
			.orElseThrow(() -> new BusinessException(AuthExceptionCode.INVALID_CREDENTIALS));
		return issueTokens(member);
	}

	/**등급·닉네임·프로필은 리프레시 토큰 claim이 아니라 DB에서 다시 읽는다.
	 * claim을 그대로 쓰면 제재(WARNING)나 등급 변경이 리프레시 토큰 만료 전까지 반영되지 않는다.*/
	@Transactional(readOnly = true)
	public String refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(AuthExceptionCode.TOKEN_MISSING);
		}

		UUID memberId = UUID.fromString(jwtProvider.parseRefresh(refreshToken).getSubject());
		refreshTokenService.verify(memberId, refreshToken);

		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(AuthExceptionCode.TOKEN_INVALID));

		return jwtProvider.createAccessToken(
			member.getId(), member.getNickname(), member.authority(), member.getProfileImg());
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
