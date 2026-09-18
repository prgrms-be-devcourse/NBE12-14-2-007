package com.team007.room_escape.domain.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.team007.room_escape.global.config.AuthMailProperties;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.AuthExceptionCode;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 이메일 인증 코드 발급·검증.
 *
 * 절차는 세 단계다.
 *   1) issueCode   : 코드를 만들어 캐시에 담는다
 *   2) verifyCode  : 코드가 맞는지 확인하고 "인증됨"으로 표시한다
 *   3) consumeVerified : 인증된 상태를 소모한다(실제 작업 직전에 호출)
 *
 * 코드와 새 비밀번호를 한 번에 받지 않고 나눈 이유는,
 * 코드가 틀렸을 때 비밀번호까지 다시 입력하게 만들지 않기 위해서다.
 *
 * 저장소는 로컬 캐시라서 앱을 재시작하면 진행 중이던 인증은 모두 무효가 되고,
 * 서버를 2대 이상 띄우면 동작하지 않는다. 확장이 필요하면 이 클래스만 Redis로 갈아끼운다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int CODE_BOUND = 1_000_000;
	private static final long MAX_ENTRIES = 10_000;

	/** 인증 코드의 용도. 회원당 용도별로 1건만 살아있다. */
	public enum Purpose {
		/** 비밀번호 변경 전 본인 확인 */
		PASSWORD_CHANGE
	}

	private final AuthMailProperties properties;

	private Cache<String, Entry> cache;

	@PostConstruct
	void initCache() {
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofMinutes(properties.cacheRetentionMinutes()))
			.maximumSize(MAX_ENTRIES)
			.build();
	}

	/**
	 * @param verified 코드 확인을 통과했는지. 통과 후에는 코드를 다시 묻지 않는다
	 * @param expiresAt 단계마다 다른 만료 시각. 발급 직후엔 코드 만료, 인증 후엔 작업 제한시간
	 */
	private record Entry(String code, LocalDateTime issuedAt, LocalDateTime expiresAt, boolean verified) {

		Entry markVerified(LocalDateTime expiresAt) {
			return new Entry(code, issuedAt, expiresAt, true);
		}
	}

	/**
	 * 인증 코드를 발급하고 그 코드를 돌려준다. 메일 발송은 호출한 쪽에서 한다.
	 *
	 * @throws BusinessException 재발송 제한 시간이 지나지 않은 경우
	 */
	public String issueCode(UUID memberId, Purpose purpose) {
		LocalDateTime now = LocalDateTime.now();
		String key = key(memberId, purpose);

		Entry existing = cache.getIfPresent(key);
		if (existing != null && isInCooldown(existing, now)) {
			throw new BusinessException(AuthExceptionCode.VERIFICATION_RESEND_TOO_SOON);
		}

		String code = generateCode();
		cache.put(key, new Entry(code, now, now.plusMinutes(properties.expireMinutes()), false));

		return code;
	}

	/**
	 * 1단계. 코드가 맞는지 확인하고 인증 상태로 바꾼다.
	 * 이 시점부터 verifiedValidMinutes 동안 실제 작업을 진행할 수 있다.
	 */
	public void verifyCode(UUID memberId, Purpose purpose, String inputCode) {
		LocalDateTime now = LocalDateTime.now();
		String key = key(memberId, purpose);
		Entry entry = requireAlive(key, now);

		if (!entry.code().equals(inputCode)) {
			throw new BusinessException(AuthExceptionCode.VERIFICATION_CODE_MISMATCH);
		}

		cache.put(key, entry.markVerified(now.plusMinutes(properties.verifiedValidMinutes())));
	}

	/**
	 * 2단계. 인증을 통과한 상태인지 확인하고 소모한다.
	 * 한 번 인증으로 작업을 여러 번 수행하지 못하도록 즉시 폐기한다.
	 */
	public void consumeVerified(UUID memberId, Purpose purpose) {
		String key = key(memberId, purpose);
		Entry entry = requireAlive(key, LocalDateTime.now());

		if (!entry.verified()) {
			throw new BusinessException(AuthExceptionCode.VERIFICATION_REQUIRED);
		}

		cache.invalidate(key);
	}

	/** 존재하고 아직 만료되지 않은 항목을 돌려준다. */
	private Entry requireAlive(String key, LocalDateTime now) {
		Entry entry = cache.getIfPresent(key);
		if (entry == null) {
			throw new BusinessException(AuthExceptionCode.VERIFICATION_NOT_FOUND);
		}
		if (entry.expiresAt().isBefore(now)) {
			cache.invalidate(key);
			throw new BusinessException(AuthExceptionCode.VERIFICATION_CODE_EXPIRED);
		}
		return entry;
	}

	private boolean isInCooldown(Entry entry, LocalDateTime now) {
		return entry.issuedAt().plusSeconds(properties.resendCooldownSeconds()).isAfter(now);
	}

	/** 000000 ~ 999999 범위의 6자리 코드. 앞자리 0도 유지한다. */
	private String generateCode() {
		return "%06d".formatted(RANDOM.nextInt(CODE_BOUND));
	}

	private String key(UUID memberId, Purpose purpose) {
		return memberId + ":" + purpose.name();
	}
}
