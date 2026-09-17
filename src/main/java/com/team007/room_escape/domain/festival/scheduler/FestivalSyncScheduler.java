package com.team007.room_escape.domain.festival.scheduler;

import com.team007.room_escape.domain.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

	private final FestivalService festivalService;

	/** yml 파일에 작성된 cron 값을 가져다 사용 / Scheduled : 정해진 시간마다 메서드 자동 실행 어노테이션**/
	@Scheduled(cron = "${public-api.festival.sync-cron}")
	public void syncFestivals() {
		festivalService.syncPublicFestivals();
	}
}
