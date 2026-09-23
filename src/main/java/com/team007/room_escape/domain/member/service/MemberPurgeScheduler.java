package com.team007.room_escape.domain.member.service;

import java.time.LocalDateTime;

import com.team007.room_escape.domain.member.infra.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보관 기간이 지난 탈퇴 회원의 개인정보를 파기하는 배치.
 *
 * 개인정보보호법은 처리 목적을 달성하면 지체 없이 파기하도록 한다.
 * 탈퇴한 회원의 이메일·전화번호를 계속 갖고 있을 근거가 없으므로 기간을 두고 지운다.
 *
 * 이 배치가 돌면 이메일 자리가 비워지므로, 그때부터 같은 이메일로 다시 가입할 수 있다.
 * 즉 보관 기간이 곧 재가입 차단 기간이다.
 */
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

		// 0건일 때도 남긴다. 배치가 돌고 있는지 확인할 방법이 로그뿐이다.
		log.info("[privacy] 탈퇴 회원 개인정보 파기 {}건 (기준 {}개월, {} 이전)",
			purged, retentionMonths, threshold);
	}
}
