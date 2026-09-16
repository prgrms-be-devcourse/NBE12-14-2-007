package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.infra.client.FestivalPublicApiClient;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalServiceTest {

	@Mock
	private FestivalRepository festivalRepository;

	@Mock
	private PublicFestivalSourceRepository publicFestivalSourceRepository;

	@Mock
	private FestivalPublicApiClient festivalPublicApiClient;

	@InjectMocks
	private FestivalService festivalService;

	private FestivalApiRow row(String beginDe, String endDe, String writngDe) {
		return new FestivalApiRow(
			"경기문화재단", "남한산성문화제", "행사", "https://example.com/1",
			"10:00~18:00", "무료", "031-000-0000", "광주시",
			"https://example.com", "https://example.com/img.jpg",
			beginDe, endDe, writngDe
		);
	}

	@Test
	@DisplayName("전체 건수와 DB 저장 건수가 같으면 아무것도 저장하지 않는다")
	void doesNotSaveWhenNoNewFestivals() {
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(10, List.of()));
		when(festivalRepository.countByProviderType(ProviderType.PUBLIC)).thenReturn(10L);

		festivalService.syncPublicFestivals();

		verify(festivalPublicApiClient, times(1)).fetch(1, 1);
		verify(festivalRepository, never()).saveAll(any());
	}

	@Test
	@DisplayName("신규 건수(N)만큼만 다시 조회해서 저장한다")
	void fetchesAndSavesOnlyNNewFestivals() {
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(3, List.of()));
		when(festivalRepository.countByProviderType(ProviderType.PUBLIC)).thenReturn(1L);
		when(festivalPublicApiClient.fetch(1, 2))
			.thenReturn(new FestivalApiResult(3, List.of(
				row("20260918", "29991231", "20260908"),
				row("20260919", "29991231", "20260908")
			)));

		festivalService.syncPublicFestivals();

		verify(festivalPublicApiClient).fetch(1, 2);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Festival>> captor = ArgumentCaptor.forClass(List.class);
		verify(festivalRepository).saveAll(captor.capture());
		assertThat(captor.getValue()).hasSize(2);
	}

	@Test
	@DisplayName("API 응답 필드가 Festival 엔티티 필드로 올바르게 매핑된다")
	void mapsApiRowFieldsToFestivalEntity() {
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of()));
		when(festivalRepository.countByProviderType(ProviderType.PUBLIC)).thenReturn(0L);
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of(row("20260918", "29991231", "20260908"))));

		festivalService.syncPublicFestivals();

		Festival festival = captureSaved().get(0);
		assertThat(festival.getProviderType()).isEqualTo(ProviderType.PUBLIC);
		assertThat(festival.getInstNm()).isEqualTo("경기문화재단");
		assertThat(festival.getTitle()).isEqualTo("남한산성문화제");
		assertThat(festival.getCategory()).isEqualTo("행사");
		assertThat(festival.getUrl()).isEqualTo("https://example.com/1");
		assertThat(festival.getImgUrl()).isEqualTo("https://example.com/img.jpg");
		assertThat(festival.getHostInstNm()).isEqualTo("광주시");
		assertThat(festival.getBeginDe()).isEqualTo(LocalDateTime.of(2026, 9, 18, 0, 0));
		assertThat(festival.getEndDe()).isEqualTo(LocalDateTime.of(2999, 12, 31, 0, 0));
		assertThat(festival.getWritngDe()).isEqualTo(LocalDateTime.of(2026, 9, 8, 0, 0));
	}

	@Test
	@DisplayName("종료일이 지난 행사는 CLOSED 상태로 저장된다")
	void resolvesClosedStatusWhenEndDeIsInThePast() {
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of()));
		when(festivalRepository.countByProviderType(ProviderType.PUBLIC)).thenReturn(0L);
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of(row("20200101", "20200102", "20200101"))));

		festivalService.syncPublicFestivals();

		assertThat(captureSaved().get(0).getStatus()).isEqualTo(FestivalStatus.CLOSED);
	}

	@Test
	@DisplayName("종료일이 비어 있으면 날짜는 null이고 상태는 OPEN으로 저장된다")
	void handlesBlankDateAsNullAndDefaultsToOpen() {
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of()));
		when(festivalRepository.countByProviderType(ProviderType.PUBLIC)).thenReturn(0L);
		when(festivalPublicApiClient.fetch(1, 1))
			.thenReturn(new FestivalApiResult(1, List.of(row("20260918", "", "20260908"))));

		festivalService.syncPublicFestivals();

		Festival festival = captureSaved().get(0);
		assertThat(festival.getEndDe()).isNull();
		assertThat(festival.getStatus()).isEqualTo(FestivalStatus.OPEN);
	}

	@SuppressWarnings("unchecked")
	private List<Festival> captureSaved() {
		ArgumentCaptor<List<Festival>> captor = ArgumentCaptor.forClass(List.class);
		verify(festivalRepository).saveAll(captor.capture());
		return captor.getValue();
	}
}
