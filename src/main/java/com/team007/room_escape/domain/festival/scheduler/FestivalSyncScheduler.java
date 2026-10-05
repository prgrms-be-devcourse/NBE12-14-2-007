package com.team007.room_escape.domain.festival.scheduler;

import com.team007.room_escape.domain.festival.service.FestivalSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

	private final FestivalSyncExecutor festivalSyncExecutor;

	/** yml의 cron 값마다 자동 실행한다. */
	@Scheduled(cron = "${public-api.festival.sync-cron}")
	public void syncFestivals() {
		// 수동 동기화가 진행 중이면 끝날 때까지 기다렸다가 이번 회차도 실행한다.
		festivalSyncExecutor.run();
	}
}
