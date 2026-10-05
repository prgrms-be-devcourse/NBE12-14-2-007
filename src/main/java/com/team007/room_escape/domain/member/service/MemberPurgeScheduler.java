package com.team007.room_escape.domain.member.service;

import java.time.LocalDateTime;

import com.team007.room_escape.domain.member.infra.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 보관 기간이 지난 탈퇴 회원의 개인정보를 파기한다. 보관 기간이 곧 재가입 차단 기간이다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeScheduler {

	private final MemberRepository memberRepository;

	/** 탈퇴 후 개인정보를 보관하는 기간(개월). */
	@Value("${privacy.withdrawn-retention-months:6}")
	private int retentionMonths;

	@Scheduled(cron = "${privacy.purge-cron:0 30 4 * * *}")
	@Transactional
	public void purgeWithdrawnMembers() {
		LocalDateTime threshold = LocalDateTime.now().minusMonths(retentionMonths);

		int purged = memberRepository.purgeWithdrawnBefore(threshold);

		// 배치가 도는지 확인하려고 0건이어도 남긴다.
		log.info("[privacy] 탈퇴 회원 개인정보 파기 {}건 (기준 {}개월, {} 이전)",
			purged, retentionMonths, threshold);
	}
}
