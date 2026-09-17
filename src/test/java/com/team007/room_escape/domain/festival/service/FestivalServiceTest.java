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

/**
 * FestivalService.syncPublicFestivals()의 배치 동기화 로직을 검증
 * 외부 API(FestivalPublicApiClient)와 DB(FestivalRepository)는 Mockito로 목 처리해서 실제 네트워크 호출/DB 접근 없이 순수 로직만 확인
 */
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

	/**
	 * 테스트마다 반복되는 FestivalApiRow 생성을 줄이기 위한 헬퍼.
	 * 날짜 3개(beginDe, endDe, writngDe)만 테스트별로 바꿔가며 나머지 필드는 고정값 사용 */
	private FestivalApiRow row(String beginDe, String endDe, String writngDe) {
		return new FestivalApiRow(
			"경기문화재단", "남한산성문화제", "행사", "https://example.com/1",
			"10:00~18:00", "무료", "031-000-0000", "광주시",
			"https://example.com", "https://example.com/img.jpg",
			beginDe, endDe, writngDe
		);
	}

	/**
	 * N = 전체 건수 - DB 저장 건수가 0 이하인 경우, saveAll이 아예 호출하면 안됨
	 * fetch(1, 1)은 totalCount 확인용으로 한 번만 호출되고, 그 이후엔 더 호출 X
	 */
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

	/**
	 * 전체 3건, DB 저장 1건 -> N=2. fetch(1, 1)로 totalCount만 먼저 확인
	 * -> fetch(1, 2)로 N건만 다시 요청해서 그대로 saveAll에 넘기는지 확인
	 */
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

	/**
	 * DB 저장 건수 0, 전체 건수 1이라 N=1 -> 두 번째 호출도 fetch(1, 1)이 되므로
	 * 같은 인자에 대해 stub을 두 번 걸어 순서대로(첫 호출: 카운트용 빈 목록, 두 번째: 실제 row) 반환하게 한다.
	 * toFestival()이 각 필드를 제대로 옮기고 yyyyMMdd 문자열도 LocalDateTime으로 잘 파싱하는지 확인한다.
	 */
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

	/**
	 * endDe를 확실히 과거인 날짜(2020-01-02)로 줘서, 테스트를 언제 실행하든
	 * resolveStatus()가 CLOSED를 반환하는지 안정적으로 확인한다.
	 */
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

	/**
	 * 실제 API 응답에서도 EVENT_TM_INFO 등 일부 필드가 빈 값으로 오는 걸 확인했었는데,
	 * 날짜 필드가 빈 문자열("")로 와도 parseDate()가 예외 없이 null을 반환하고,
	 * resolveStatus(null)은 기본값 OPEN을 반환하는지 확인한다.
	 */
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

	/** saveAll(List)에 실제로 넘어간 Festival 목록을 잡아내는 공통 헬퍼. */
	@SuppressWarnings("unchecked")
	private List<Festival> captureSaved() {
		ArgumentCaptor<List<Festival>> captor = ArgumentCaptor.forClass(List.class);
		verify(festivalRepository).saveAll(captor.capture());
		return captor.getValue();
	}
}
