package com.team007.room_escape.domain.festival;

import static org.assertj.core.api.Assertions.assertThat;
import com.team007.room_escape.domain.festival.infra.entity.*;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubmissionDuplicateRepositoryTest {
	@Autowired EntityManager em;
	@Autowired FestivalRepository festivals;

	@Test
	void comparesCalendarDatesNormalizesStoredUrlAndExcludesDeletedAndSelf() {
		LocalDateTime start = LocalDateTime.of(2026, 10, 2, 0, 0);
		LocalDateTime end = start.plusDays(1);
		String url = "https://example.com/" + UUID.randomUUID();
		Festival festival = Festival.builder().providerType(ProviderType.MEMBER)
				.url(" " + url + "/// ").region(FestivalRegion.GYEONGGI)
				.beginDe(start.plusHours(10)).endDe(end.plusHours(18)).build();
		em.persist(festival);
		em.flush();
		assertThat(festivals.lockSubmissionDuplicateKey(123456789L)).isEqualTo(1);
		assertThat(duplicate(null, start, end, url)).isTrue();
		assertThat(duplicate(festival.getId(), start, end, url)).isFalse();
		assertThat(duplicate(null, start.plusDays(1), end, url)).isFalse();
		assertThat(duplicate(null, start, end.plusDays(1), url)).isFalse();
		assertThat(duplicate(null, start, end, url + "different")).isFalse();
		assertThat(festivals.existsSubmissionDuplicate(null, start, start.plusDays(1),
				end, end.plusDays(1), FestivalRegion.SEOUL.name(), url)).isFalse();
		festival.delete();
		em.flush();
		assertThat(duplicate(null, start, end, url)).isFalse();
	}

	private boolean duplicate(Long id, LocalDateTime start, LocalDateTime end, String url) {
		return festivals.existsSubmissionDuplicate(id, start, start.plusDays(1),
				end, end.plusDays(1), FestivalRegion.GYEONGGI.name(), url);
	}
}
