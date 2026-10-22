package com.team007.room_escape.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 파기 배치가 저장소에 넘기는 기준 시각을 검증한다.
 * 기준 시각이 미래가 되면 탈퇴 회원 전체가 한 번에 파기되므로
 * 방향과 보관 기간 반영을 쿼리와 분리해 확인한다.
 */
class MemberPurgeSchedulerTest {

	private static final int RETENTION_MONTHS = 6;

	private MemberRepository memberRepository;
	private MemberPurgeScheduler scheduler;

	@BeforeEach
	void setUp() {
		memberRepository = mock(MemberRepository.class);
		scheduler = new MemberPurgeScheduler(memberRepository);
		// @Value 는 스프링 컨텍스트 없이는 채워지지 않아 직접 넣는다.
		ReflectionTestUtils.setField(scheduler, "retentionMonths", RETENTION_MONTHS);
	}

	@Test
	void 보관_기간만큼_과거_시각을_기준으로_파기를_요청한다() {
		when(memberRepository.purgeWithdrawnBefore(any())).thenReturn(0);
		LocalDateTime before = LocalDateTime.now();

		scheduler.purgeWithdrawnMembers();

		ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(memberRepository).purgeWithdrawnBefore(threshold.capture());
		assertThat(threshold.getValue())
			.isAfterOrEqualTo(before.minusMonths(RETENTION_MONTHS))
			.isBeforeOrEqualTo(LocalDateTime.now().minusMonths(RETENTION_MONTHS));
	}

	/** 기준 시각이 현재보다 미래면 탈퇴 직후 회원까지 파기된다. */
	@Test
	void 기준_시각은_반드시_현재보다_과거다() {
		when(memberRepository.purgeWithdrawnBefore(any())).thenReturn(0);

		scheduler.purgeWithdrawnMembers();

		ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(memberRepository).purgeWithdrawnBefore(threshold.capture());
		assertThat(threshold.getValue()).isBefore(LocalDateTime.now());
	}

	@Test
	void 파기할_회원이_없어도_정상_종료한다() {
		when(memberRepository.purgeWithdrawnBefore(any())).thenReturn(0);

		assertThatCode(() -> scheduler.purgeWithdrawnMembers()).doesNotThrowAnyException();
	}
}
