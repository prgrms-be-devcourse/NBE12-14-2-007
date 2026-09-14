package com.team007.room_escape.domain.auth.service;

import com.team007.room_escape.domain.auth.infra.repository.RefreshTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

	private final RefreshTokenRepository refreshTokenRepository;

	@Scheduled(cron = "${jwt.refresh-cleanup-cron:0 0 4 * * *}")
	@Transactional
	public void purgeExpired() {
		refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
		log.info("[refresh-token] 만료된 리프레시 토큰 정리 완료");
	}
}
