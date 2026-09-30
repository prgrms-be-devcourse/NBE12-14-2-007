package com.team007.room_escape.domain.festival;

import static org.assertj.core.api.Assertions.assertThat;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 기본 정렬(오늘 기준 가까운 시작일순) 쿼리를 실제 PostgreSQL 에서 검증한다.
 * 로컬 DB에 다른 데이터가 있어도 섞이지 않게, 테스트마다 고유 토큰을 제목에 넣고 그 토큰으로 검색한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NearestStartSortRepositoryTest {

	private static final LocalDateTime TODAY = LocalDateTime.of(2026, 9, 30, 0, 0);

	@Autowired
	EntityManager em;

	@Autowired
	FestivalRepository festivalRepository;

	private String token;

	@BeforeEach
	void setUp() {
		token = "nearest" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	@DisplayName("오늘 이후 시작 행사를 가까운 순으로 먼저, 이미 시작한 행사는 최근 시작순으로 뒤에 둔다")
	void ordersUpcomingFirstThenRecentlyStarted() {
		festival("long-ago", TODAY.minusMonths(6));
		festival("far-future", TODAY.plusMonths(2));
		festival("yesterday", TODAY.minusDays(1));
		festival("today", TODAY.plusHours(10));
		festival("next-week", TODAY.plusDays(7));

		Page<Festival> page = search(PageRequest.of(0, 10));

		assertThat(titles(page)).containsExactly(
				title("today"),
				title("next-week"),
				title("far-future"),
				title("yesterday"),
				title("long-ago"));
	}

	@Test
	@DisplayName("페이지를 나눠도 순서가 이어지고 전체 건수가 맞다")
	void pagesKeepOrderAndCount() {
		festival("a", TODAY.plusDays(1));
		festival("b", TODAY.plusDays(2));
		festival("c", TODAY.minusDays(1));

		Page<Festival> second = search(PageRequest.of(1, 2));

		assertThat(titles(second)).containsExactly(title("c"));
		assertThat(second.getTotalElements()).isEqualTo(3);
	}

	private Page<Festival> search(PageRequest pageable) {
		em.flush();
		return festivalRepository.searchFestivalsOrderByNearestStart(
				true, token,
				false, null,
				false, null,
				false, null,
				false, null, null,
				false,
				TODAY,
				pageable);
	}

	private void festival(String name, LocalDateTime beginDe) {
		em.persist(Festival.builder()
				.providerType(ProviderType.PUBLIC)
				.title(title(name))
				.beginDe(beginDe)
				.endDe(beginDe.plusDays(3))
				.region(FestivalRegion.values()[0])
				.build());
	}

	private String title(String name) {
		return token + "-" + name;
	}

	private List<String> titles(Page<Festival> page) {
		return page.getContent().stream().map(Festival::getTitle).toList();
	}
}
