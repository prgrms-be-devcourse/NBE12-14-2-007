package com.team007.room_escape.domain.member.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * 탈퇴 회원 개인정보 파기 쿼리를 실제 PostgreSQL 에서 검증한다.
 * 되돌릴 수 없는 대량 UPDATE 라서, 비워야 할 행만 비우는지가 핵심이다.
 *
 * 로컬 DB(.env)에 붙고 테스트마다 트랜잭션이 롤백돼 데이터가 남지 않는다.
 * 로컬 DB의 다른 회원도 같은 조건이면 함께 갱신되므로, 반환 건수로 단정하는 대신
 * 이 테스트가 만든 회원의 상태를 직접 확인한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberPurgeRepositoryTest {

	private static final int RETENTION_MONTHS = 6;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);
	private static final LocalDateTime THRESHOLD = NOW.minusMonths(RETENTION_MONTHS);

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 보관_기간이_지난_탈퇴_회원의_개인정보를_비운다() {
		Member member = withdrawnAt(THRESHOLD.minusDays(1));

		memberRepository.purgeWithdrawnBefore(THRESHOLD);

		Member purged = reload(member);
		assertThat(purged.getEmail()).isEqualTo("purged-" + member.getId() + "@removed.local");
		assertThat(purged.getNickname()).isEqualTo(Member.WITHDRAWN_NICKNAME);
		assertThat(purged.getPhone()).isNull();
		assertThat(purged.getProfileImg()).isNull();
	}

	@Test
	void 글이_참조하므로_파기해도_회원_행은_남는다() {
		Member member = withdrawnAt(THRESHOLD.minusDays(1));

		memberRepository.purgeWithdrawnBefore(THRESHOLD);

		assertThat(memberRepository.findById(member.getId())).isPresent();
	}

	@Test
	void 보관_기간이_남은_탈퇴_회원은_건드리지_않는다() {
		Member member = withdrawnAt(THRESHOLD.plusDays(1));
		String email = member.getEmail();

		memberRepository.purgeWithdrawnBefore(THRESHOLD);

		Member kept = reload(member);
		assertThat(kept.getEmail()).isEqualTo(email);
		assertThat(kept.getNickname()).isNotEqualTo(Member.WITHDRAWN_NICKNAME);
	}

	/**
	 * 기준 시각 계산이 뒤집히면 탈퇴하지 않은 회원까지 파기된다.
	 * 되돌릴 수 없는 사고라서 따로 확인한다.
	 */
	@Test
	void 탈퇴하지_않은_회원은_아무리_오래되어도_파기하지_않는다() {
		Member member = save("활동 중", false);
		String email = member.getEmail();
		String nickname = member.getNickname();

		memberRepository.purgeWithdrawnBefore(THRESHOLD);

		Member kept = reload(member);
		assertThat(kept.getEmail()).isEqualTo(email);
		assertThat(kept.getNickname()).isEqualTo(nickname);
		assertThat(kept.getPhone()).isNotNull();
	}

	/** 이미 파기한 회원을 매일 다시 세지 않는다. 같은 기준으로 또 돌리면 대상이 없어야 한다. */
	@Test
	void 이미_파기된_회원은_다시_대상이_되지_않는다() {
		withdrawnAt(THRESHOLD.minusDays(1));

		int first = memberRepository.purgeWithdrawnBefore(THRESHOLD);
		int second = memberRepository.purgeWithdrawnBefore(THRESHOLD);

		assertThat(first).isPositive();
		assertThat(second).isZero();
	}

	/** deletedAt 은 delete() 가 현재 시각으로만 넣어서, 경계 검증을 위해 직접 지정한다. */
	private Member withdrawnAt(LocalDateTime deletedAt) {
		Member member = save("탈퇴 예정", true);
		entityManager.createQuery("update Member m set m.deletedAt = :at where m.id = :id")
			.setParameter("at", deletedAt)
			.setParameter("id", member.getId())
			.executeUpdate();
		entityManager.clear();
		return member;
	}

	private Member save(String label, boolean willWithdraw) {
		String token = UUID.randomUUID().toString().substring(0, 8);
		Member member = memberRepository.saveAndFlush(Member.builder()
			.email(UUID.randomUUID() + "@example.com")
			.password("test")
			.role(MemberRole.ROLE_UNVERIFIED)
			.nickname(label + " " + token)
			.phone("010-0000-0000")
			.profileImg("profile/" + token + ".png")
			.build());
		if (!willWithdraw) {
			entityManager.clear();
		}
		return member;
	}

	private Member reload(Member member) {
		entityManager.clear();
		return memberRepository.findById(member.getId()).orElseThrow();
	}
}
