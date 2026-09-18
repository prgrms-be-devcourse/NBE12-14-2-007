package com.team007.room_escape.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 auth-mail.* 설정 바인딩.
 *
 * @param expireMinutes         인증 코드 유효시간(분)
 * @param resendCooldownSeconds 재발송 제한(초)
 * @param verifiedValidMinutes  인증 통과 후 실제 작업(비밀번호 변경)까지 허용되는 시간(분)
 */
@ConfigurationProperties(prefix = "auth-mail")
public record AuthMailProperties(
	long expireMinutes,
	long resendCooldownSeconds,
	long verifiedValidMinutes
) {

	/** 캐시 보관 시간. 두 단계 중 긴 쪽보다 넉넉해야 "만료"와 "없음"을 구분할 수 있다. */
	public long cacheRetentionMinutes() {
		return Math.max(expireMinutes, verifiedValidMinutes) + 1;
	}
}
