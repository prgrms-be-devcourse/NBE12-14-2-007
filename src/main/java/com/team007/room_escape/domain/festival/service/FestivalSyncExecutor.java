package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공공 행사 동기화가 동시에 두 번 실행되지 않게 막는 실행기.
 * 스케줄러와 수동 동기화 버튼이 모두 이 클래스를 거친다.
 * 동기화 전체가 끝나서 신규 저장까지 커밋된 뒤에 잠금이 풀려야
 * 다음 실행이 최신 저장 건수를 보고 중복 저장하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FestivalSyncExecutor {

	private final FestivalService festivalService;

	private final ReentrantLock lock = new ReentrantLock();

	/** 수동 동기화용: 이미 실행 중이면 기다리지 않고 empty를 돌려준다 (컨트롤러가 409로 응답). */
	public Optional<FestivalResponse.SyncResponse> tryRun() {
		if (!lock.tryLock()) {
			return Optional.empty();
		}
		try {
			return Optional.of(festivalService.syncPublicFestivals());
		} finally {
			lock.unlock(); // 예외가 나도 반드시 풀어야 이후 동기화가 막히지 않는다
		}
	}

	/** 스케줄러용: 이미 실행 중이면 끝날 때까지 기다렸다가 이번 회차도 실행한다. */
	public FestivalResponse.SyncResponse run() {
		lock.lock();
		try {
			return festivalService.syncPublicFestivals();
		} finally {
			lock.unlock();
		}
	}
}
