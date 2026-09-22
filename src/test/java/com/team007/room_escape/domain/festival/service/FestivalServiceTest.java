package com.team007.room_escape.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.infra.client.FestivalPublicApiClient;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

/**
 * FestivalService.syncPublicFestivals()의 배치 동기화 로직을 검증
 * 외부 API(FestivalPublicApiClient)와 DB(FestivalRepository 등)는 Mockito로 목 처리해서
 * 실제 네트워크 호출/DB 접근 없이 순수 로직만 확인
 */
@ExtendWith(MockitoExtension.class)
class FestivalServiceTest {

	private static final int CURRENT_YEAR = LocalDate.now().getYear();
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	@Mock
	private FestivalRepository festivalRepository;

	@Mock
	private PublicFestivalSourceRepository publicFestivalSourceRepository;

	@Mock
	private FestivalPublicApiClient festivalPublicApiClient;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private FestivalService festivalService;

	/** 올해 날짜(currentYear+MMdd)를 가진 FestivalApiRow를 만드는 헬퍼 */
	private FestivalApiRow rowInCurrentYear(String monthDay, String endDe) {
		return row(CURRENT_YEAR + monthDay, endDe, CURRENT_YEAR + "0101");
	}

	/** 날짜 3개(beginDe, endDe, writngDe)만 테스트별로 바꿔가며 나머지 필드는 고정값을 쓴다. */
	private FestivalApiRow row(String beginDe, String endDe, String writngDe) {
		return new FestivalApiRow(
			"경기문화재단", "남한산성문화제", "행사", "https://example.com/1",
			"10:00~18:00", "무료", "031-000-0000", "광주시",
			"https://example.com", "https://example.com/img.jpg",
			beginDe, endDe, writngDe
		);
	}

	/**
	 * FestivalService가 연도 범위를 계산해서 넘기는 정확한 LocalDateTime 값을 여기서 재계산하지 않고,
	 * any(LocalDateTime.class)로 느슨하게 매칭해서 "DB에 이만큼 저장돼 있다고 치자"만 표현한다.
	 */
	private void stubYearCount(long dbCount) {
		when(festivalRepository.countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
			eq(ProviderType.PUBLIC), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(dbCount);
	}

	/**
	 * 전체 응답이 한 페이지(1000건 미만)에 다 들어오는 가장 단순한 경우.
	 * 올해 행사 1건, DB에 0건 저장돼 있으면 N=1이라 신규로 저장돼야 한다.
	 */
	@Test
	@DisplayName("올해 필터링된 건수와 DB 건수가 같으면 아무것도 저장하지 않는다")
	void doesNotSaveWhenNoNewFestivals() {
		FestivalApiRow existing = rowInCurrentYear("0918", "20261231");
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(existing)));
		stubYearCount(1L);

		festivalService.syncPublicFestivals();

		verify(festivalPublicApiClient, times(1)).fetch(1, 1000);
		verify(festivalRepository, never()).saveAll(any());
	}

	/**
	 * totalCount가 1000을 넘으면 fetchAllRows()가 다음 페이지(pIndex=2)까지 이어서 호출해야 한다.
	 */
	@Test
	@DisplayName("전체 건수가 1000건을 넘으면 페이지를 나눠서 전부 가져온다")
	void fetchesMultiplePagesWhenTotalCountExceedsPageSize() {
		FestivalApiRow row1 = rowInCurrentYear("0101", "29991231");
		FestivalApiRow row2 = rowInCurrentYear("0102", "29991231");

		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1001, dummyRows(1000, row1)));
		when(festivalPublicApiClient.fetch(2, 1000))
			.thenReturn(new FestivalApiResult(1001, List.of(row2)));
		stubYearCount(0L);

		festivalService.syncPublicFestivals();

		verify(festivalPublicApiClient).fetch(1, 1000);
		verify(festivalPublicApiClient).fetch(2, 1000);
	}

	/** 페이지네이션 테스트용으로 같은 row를 count번 반복한 목록을 만든다 (내용보다 "1000건 채웠다"는 게 중요). */
	private List<FestivalApiRow> dummyRows(int count, FestivalApiRow sample) {
		return java.util.stream.Stream.generate(() -> sample).limit(count).toList();
	}

	/**
	 * BEGIN_DE가 올해가 아닌 행사는 신규 건수(N) 계산과 저장 대상에서 제외돼야 한다.
	 */
	@Test
	@DisplayName("BEGIN_DE가 올해가 아닌 행사는 걸러진다")
	void filtersOutRowsNotInCurrentYear() {
		FestivalApiRow thisYear = rowInCurrentYear("0918", "29991231");
		FestivalApiRow otherYear = row((CURRENT_YEAR - 1) + "1231", "29991231", (CURRENT_YEAR - 1) + "1201");

		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(2, List.of(thisYear, otherYear)));
		stubYearCount(0L);

		festivalService.syncPublicFestivals();

		assertThat(captureSaved()).hasSize(1);
	}

	/**
	 * toGyeonggiFestival()이 각 필드를 제대로 옮기고 yyyyMMdd 문자열도 LocalDateTime으로 잘 파싱하는지 확인한다.
	 */
	@Test
	@DisplayName("API 응답 필드가 Festival 엔티티 필드로 올바르게 매핑된다")
	void mapsApiRowFieldsToFestivalEntity() {
		FestivalApiRow row = rowInCurrentYear("0918", "29991231");
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(row)));
		stubYearCount(0L);

		festivalService.syncPublicFestivals();

		Festival festival = captureSaved().get(0);
		assertThat(festival.getProviderType()).isEqualTo(ProviderType.PUBLIC);
		assertThat(festival.getInstNm()).isEqualTo("경기문화재단");
		assertThat(festival.getTitle()).isEqualTo("남한산성문화제");
		assertThat(festival.getCategory()).isEqualTo("행사");
		assertThat(festival.getUrl()).isEqualTo("https://example.com/1");
		assertThat(festival.getImgUrl()).isEqualTo("https://example.com/img.jpg");
		assertThat(festival.getHostInstNm()).isEqualTo("광주시");
		assertThat(festival.getBeginDe()).isEqualTo(LocalDateTime.of(CURRENT_YEAR, 9, 18, 0, 0));
		assertThat(festival.getEndDe()).isEqualTo(LocalDateTime.of(2999, 12, 31, 0, 0));
	}

	/**
	 * endDe를 "어제" 날짜로 동적으로 만들어서, 테스트를 언제 실행하든
	 * resolveStatus()가 CLOSED를 반환하는지 안정적으로 확인한다.
	 */
	@Test
	@DisplayName("종료일이 지난 행사는 CLOSED 상태로 저장된다")
	void resolvesClosedStatusWhenEndDeIsInThePast() {
		String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMAT);
		FestivalApiRow row = rowInCurrentYear("0101", yesterday);

		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(row)));
		stubYearCount(0L);

		festivalService.syncPublicFestivals();

		assertThat(captureSaved().get(0).getStatus()).isEqualTo(FestivalStatus.CLOSED);
	}

	/**
	 * 실제 API 응답에서도 일부 필드가 빈 값으로 오는 걸 확인했었는데,
	 * 날짜 필드가 빈 문자열("")로 와도 parseDate()가 예외 없이 null을 반환하고,
	 * resolveStatus(null)은 기본값 OPEN을 반환하는지 확인한다.
	 */
	@Test
	@DisplayName("종료일이 비어 있으면 날짜는 null이고 상태는 OPEN으로 저장된다")
	void handlesBlankDateAsNullAndDefaultsToOpen() {
		FestivalApiRow row = rowInCurrentYear("0918", "");
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(row)));
		stubYearCount(0L);

		festivalService.syncPublicFestivals();

		Festival festival = captureSaved().get(0);
		assertThat(festival.getEndDe()).isNull();
		assertThat(festival.getStatus()).isEqualTo(FestivalStatus.OPEN);
	}

	/**
	 * 원본 스냅샷은 누적하지 않고 기존 것을 지운 뒤 새로 저장해야 한다 (순서까지 확인).
	 */
	@Test
	@DisplayName("원본 데이터는 기존 것을 삭제하고 최신 것만 저장한다")
	void savesLatestRawSourceOnly() {
		FestivalApiRow row = rowInCurrentYear("0918", "29991231");
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(row)));
		stubYearCount(1L);
		when(objectMapper.writeValueAsString(Mockito.<List<FestivalApiRow>>any())).thenReturn("[]");

		festivalService.syncPublicFestivals();

		InOrder inOrder = Mockito.inOrder(publicFestivalSourceRepository);
		inOrder.verify(publicFestivalSourceRepository).deleteAll();
		inOrder.verify(publicFestivalSourceRepository).save(any());
	}

	/** 종료 처리 대상으로 조회될 (종료일이 지났는데 아직 OPEN인) 행사를 만드는 헬퍼. */
	private Festival expiredOpenFestival(Long id, String title) {
		return Festival.builder()
			.id(id)
			.providerType(ProviderType.PUBLIC)
			.title(title)
			.endDe(LocalDate.now().minusDays(1).atStartOfDay())
			.status(FestivalStatus.OPEN)
			.build();
	}

	/**
	 * 수동 동기화 버튼에 돌려줄 결과: 종료 처리한 행사 목록과 새로 저장한 행사 목록(행사명 포함)이 담기는지 확인한다.
	 */
	@Test
	@DisplayName("동기화 결과에 종료 처리된 행사 목록과 새로 저장한 행사 목록이 담긴다")
	void returnsClosedAndSavedFestivals() {
		FestivalApiRow row1 = rowInCurrentYear("0101", "29991231");
		FestivalApiRow row2 = rowInCurrentYear("0102", "29991231");
		when(festivalRepository.findExpiredOpen(any(LocalDateTime.class)))
			.thenReturn(List.of(expiredOpenFestival(3L, "지난 음악회"), expiredOpenFestival(4L, "지난 전시회")));
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(2, List.of(row1, row2)));
		stubYearCount(0L);

		FestivalResponse.SyncResponse result = festivalService.syncPublicFestivals();

		assertThat(result.closedFestivals())
			.extracting(FestivalResponse.SyncedFestival::title)
			.containsExactly("지난 음악회", "지난 전시회");
		assertThat(result.savedFestivals())
			.extracting(FestivalResponse.SyncedFestival::title)
			.containsExactly("남한산성문화제", "남한산성문화제");
	}

	/**
	 * 종료 처리 목록을 뽑은 뒤, 같은 시각(now)으로 일괄 갱신까지 호출되는지 확인한다.
	 */
	@Test
	@DisplayName("종료 대상 목록을 조회한 뒤 같은 시각으로 CLOSED 일괄 갱신을 호출한다")
	void closesExpiredFestivalsWithSameNowAsLookup() {
		FestivalApiRow existing = rowInCurrentYear("0918", "29991231");
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(existing)));
		stubYearCount(1L);

		festivalService.syncPublicFestivals();

		ArgumentCaptor<LocalDateTime> lookupNow = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> updateNow = ArgumentCaptor.forClass(LocalDateTime.class);
		InOrder inOrder = Mockito.inOrder(festivalRepository);
		inOrder.verify(festivalRepository).findExpiredOpen(lookupNow.capture());
		inOrder.verify(festivalRepository).closeExpiredFestivals(updateNow.capture());
		assertThat(updateNow.getValue()).isEqualTo(lookupNow.getValue());
	}

	/**
	 * 새 행사가 없으면 저장 목록은 비어 있지만, 종료 처리한 행사 목록은 그대로 응답해야 한다.
	 */
	@Test
	@DisplayName("새 행사가 없으면 저장 목록은 비고 종료 처리 목록만 담긴다")
	void returnsEmptySavedWhenNoNewFestivals() {
		FestivalApiRow existing = rowInCurrentYear("0918", "29991231");
		when(festivalRepository.findExpiredOpen(any(LocalDateTime.class)))
			.thenReturn(List.of(expiredOpenFestival(3L, "지난 음악회")));
		when(festivalPublicApiClient.fetch(1, 1000))
			.thenReturn(new FestivalApiResult(1, List.of(existing)));
		stubYearCount(1L);

		FestivalResponse.SyncResponse result = festivalService.syncPublicFestivals();

		assertThat(result.closedFestivals()).hasSize(1);
		assertThat(result.savedFestivals()).isEmpty();
	}

	/** saveAll(List)에 실제로 넘어간 Festival 목록을 잡아내는 공통 헬퍼. */
	@SuppressWarnings("unchecked")
	private List<Festival> captureSaved() {
		ArgumentCaptor<List<Festival>> captor = ArgumentCaptor.forClass(List.class);
		verify(festivalRepository).saveAll(captor.capture());
		return captor.getValue();
	}

	/** getPublicFestivalsByRegion() 테스트용 PUBLIC 행사 엔티티를 만드는 헬퍼. */
	private Festival publicFestival(FestivalRegion region, LocalDateTime beginDe, LocalDateTime endDe, FestivalStatus storedStatus) {
		return Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.title("경기 행사")
			.category("행사")
			.instNm("경기문화재단")
			.beginDe(beginDe)
			.endDe(endDe)
			.region(region)
			.status(storedStatus)
			.build();
	}

	/**
	 * region/date가 그대로 리포지토리 쿼리 조건(PUBLIC 고정 + date는 자정 기준)으로 위임되는지 확인한다.
	 */
	@Test
	@DisplayName("지역/날짜로 조회하면 PUBLIC 타입과 자정 기준 날짜로 리포지토리에 위임한다")
	void delegatesToRepositoryWithPublicProviderTypeAndStartOfDay() {
		LocalDate date = LocalDate.of(CURRENT_YEAR, 9, 20);
		Pageable pageable = PageRequest.of(0, 20);
		Festival festival = publicFestival(
			FestivalRegion.GYEONGGI, date.minusDays(1).atStartOfDay(), date.plusDays(1).atStartOfDay(), FestivalStatus.OPEN);
		Page<Festival> page = new PageImpl<>(List.of(festival), pageable, 1);

		when(festivalRepository.findOngoingByProviderTypeAndRegion(
			ProviderType.PUBLIC, FestivalRegion.GYEONGGI, date.atStartOfDay(), pageable))
			.thenReturn(page);

		Page<FestivalResponse.ListResponse> result =
			festivalService.getPublicFestivalsByRegion(FestivalRegion.GYEONGGI, date, pageable);

		assertThat(result.getTotalElements()).isEqualTo(1);
		verify(festivalRepository).findOngoingByProviderTypeAndRegion(
			ProviderType.PUBLIC, FestivalRegion.GYEONGGI, date.atStartOfDay(), pageable);
	}

	/**
	 * 응답 매핑(ListResponse.from)이 status를 DB에 저장된 값 그대로 안 쓰고,
	 * endDe 기준으로 다시 계산해서 담는지 확인한다 (저장된 status가 낡아있어도 응답은 항상 정확해야 함).
	 */
	@Test
	@DisplayName("응답의 status는 저장된 값이 아니라 endDe 기준으로 다시 계산된다")
	void computesStatusFromEndDeNotStoredValue() {
		Pageable pageable = PageRequest.of(0, 20);
		LocalDateTime pastEndDe = LocalDate.now().minusDays(1).atStartOfDay();
		// DB엔 낡은 값(OPEN)으로 저장돼 있지만, endDe는 이미 지난 상태
		Festival staleOpenFestival = publicFestival(
			FestivalRegion.GYEONGGI, pastEndDe.minusDays(5), pastEndDe, FestivalStatus.OPEN);
		Page<Festival> page = new PageImpl<>(List.of(staleOpenFestival), pageable, 1);

		when(festivalRepository.findOngoingByProviderTypeAndRegion(
			any(ProviderType.class), any(FestivalRegion.class), any(LocalDateTime.class), any(Pageable.class)))
			.thenReturn(page);

		Page<FestivalResponse.ListResponse> result =
			festivalService.getPublicFestivalsByRegion(FestivalRegion.GYEONGGI, LocalDate.now(), pageable);

		assertThat(result.getContent().get(0).status()).isEqualTo(FestivalStatus.CLOSED);
	}

	/**
	 * ListResponse.from()이 region을 포함한 주요 필드를 엔티티에서 그대로 옮기는지 확인한다.
	 */
	@Test
	@DisplayName("응답에 지역/제목 등 주요 필드가 그대로 매핑된다")
	void mapsFestivalFieldsToListResponse() {
		Pageable pageable = PageRequest.of(0, 20);
		LocalDate date = LocalDate.now();
		Festival festival = publicFestival(
			FestivalRegion.GYEONGGI, date.minusDays(1).atStartOfDay(), date.plusDays(1).atStartOfDay(), FestivalStatus.OPEN);
		Page<Festival> page = new PageImpl<>(List.of(festival), pageable, 1);

		when(festivalRepository.findOngoingByProviderTypeAndRegion(
			any(ProviderType.class), any(FestivalRegion.class), any(LocalDateTime.class), any(Pageable.class)))
			.thenReturn(page);

		FestivalResponse.ListResponse response =
			festivalService.getPublicFestivalsByRegion(FestivalRegion.GYEONGGI, date, pageable).getContent().get(0);

		assertThat(response.title()).isEqualTo("경기 행사");
		assertThat(response.region()).isEqualTo(FestivalRegion.GYEONGGI);
	}
}
