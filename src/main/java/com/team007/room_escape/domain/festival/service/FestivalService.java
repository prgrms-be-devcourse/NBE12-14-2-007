package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSearchRequest;
import com.team007.room_escape.domain.festival.infra.client.FestivalPublicApiClient;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.entity.PublicFestivalSource;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

	/** API가 날짜열"260916"형태로 넘겨줘서 해석하는 규칙**/
	private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final int MAX_PAGE_SIZE = 1000; // API 문서상 1회 요청 최대 건수

	private final FestivalRepository festivalRepository;
	private final PublicFestivalSourceRepository publicFestivalSourceRepository; /** 원본 저장용 **/
	private final FestivalPublicApiClient festivalPublicApiClient;
	private final ObjectMapper objectMapper;

	@Transactional
	public FestivalResponse.SyncResponse syncPublicFestivals() {
		/** API 호출과 무관하게, 기존에 저장된 행사 중 종료일이 지난 건 먼저 CLOSED로 갱신 **/
		List<FestivalResponse.SyncedFestival> closedFestivals = closeExpiredFestivals();

		List<FestivalApiRow> allRows = fetchAllRows();

		/** 신규 저장이 실패해도 원본만큼은 남기고 싶어서, 필터링/비교보다 먼저 저장 **/
		saveRawSource(allRows);

		int currentYear = LocalDate.now().getYear();
		/** BEGIN_DE가 올해로 시작하는 행사만 남김 (현재 연도를 동적으로 계산 -> 해가 바뀌어도 자동화 동작) **/
		List<FestivalApiRow> currentYearRows = allRows.stream()
			.filter(row -> isInYear(row.beginDe(), currentYear))
			.toList();

		/** "올해 필터링된 건수" vs "DB에 이미 저장된 올해 건수"를 비교해야 해서,
		 *  DB 쪽도 반드시 같은 연도 범위로 좁혀서 비교 (안 그러면 서로 다른 걸 비교하게 됨) **/
		LocalDateTime yearStart = LocalDateTime.of(currentYear, 1, 1, 0, 0);
		LocalDateTime yearEnd = yearStart.plusYears(1);
		// TODO: 서울 등 다른 지역 API 추가 시 PUBLIC 전체가 아니라 지역별로 건수를 세도록 변경 (안 그러면 다른 지역 행사가 섞여 신규 건수 계산이 틀어짐)
		long dbCount = festivalRepository.countByProviderTypeAndBeginDeGreaterThanEqualAndBeginDeLessThan(
			ProviderType.PUBLIC, yearStart, yearEnd);

		int n = (int) (currentYearRows.size() - dbCount);
		if (n <= 0) {
			log.info("신규 행사 없음 ({}년 {}건, 저장된 {}건)", currentYear, currentYearRows.size(), dbCount);
			return new FestivalResponse.SyncResponse(closedFestivals, List.of());
		}

		List<FestivalResponse.SyncedFestival> savedFestivals = List.of();
		/** 이 블록에서 예외가 나도 밖으로 던지지 않아야, 트랜잭션이 정상 종료되면서
		 *  위에서 먼저 저장해둔 원본 스냅샷(saveRawSource)이 롤백되지 않고 커밋된다 **/
		try {
			List<Festival> festivals = currentYearRows.stream()
				.limit(n) // 필터링된 목록의 앞에서부터 N건만 신규로 간주
				.map(this::toGyeonggiFestival)
				.toList();

			festivalRepository.saveAll(festivals);
			// 저장이 끝난 뒤에 만들어야 DB가 붙여준 행사 번호(id)가 채워져 있다
			savedFestivals = festivals.stream().map(FestivalResponse.SyncedFestival::from).toList();
			log.info("공공 행사 {}건 저장 완료", savedFestivals.size());
		} catch (Exception e) {
			log.error("신규 행사 저장 실패, 원본 스냅샷은 반영됨", e);
		}
		return new FestivalResponse.SyncResponse(closedFestivals, savedFestivals);
	}

	/** 검색어와 선택 필터로 공공행사와 사용자 등록 행사를 통합 검색한다*/
	@Transactional(readOnly = true)
	public Page<FestivalResponse.ListResponse> searchFestivals(
			FestivalSearchRequest request,
			Pageable pageable
	) {
		String keyword = normalize(request.keyword());
		String category = normalize(request.category());

		LocalDateTime dateStart = request.date() == null
				? null
				: request.date().atStartOfDay();

		LocalDateTime dateEnd = request.date() == null
				? null
				: request.date().plusDays(1).atStartOfDay();

		return festivalRepository.searchFestivals(
				keyword != null,
				keyword,

				request.region() != null,
				request.region(),

				request.providerType() != null,
				request.providerType(),

				category != null,
				category,

				request.date() != null,
				dateStart,
				dateEnd,
				Boolean.TRUE.equals(request.excludeClosed()),

				pageable
		).map(FestivalResponse.ListResponse::from);
	}

	/** 공공 행사 1건의 상세 정보를 조회. 없거나 삭제됐거나 공공 행사가 아니면 FESTIVAL_NOT_FOUND **/
	@Transactional(readOnly = true)
	public FestivalResponse.DetailResponse getFestival(Long festivalId) {
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
		return FestivalResponse.DetailResponse.from(festival);
	}

	/** 종료일이 지났는데도 OPEN으로 남아있는 행사를 CLOSED로 일괄 갱신하고, 갱신된 행사 목록을 돌려준다 **/
	private List<FestivalResponse.SyncedFestival> closeExpiredFestivals() {
		LocalDateTime now = LocalDateTime.now();
		// 일괄 UPDATE는 어떤 행이 바뀌었는지 돌려주지 않아서, 같은 시각(now)으로 갱신 전에 목록을 먼저 뽑아둔다
		List<FestivalResponse.SyncedFestival> closed = festivalRepository.findByStatusAndEndDeBefore(FestivalStatus.OPEN, now).stream()
			.map(FestivalResponse.SyncedFestival::from)
			.toList();
		festivalRepository.closeExpiredFestivals(now);
		log.info("종료된 행사 {}건 CLOSED로 갱신", closed.size());
		return closed;
	}

	/** API 1회 요청 최대 건수를 넘는 전체 데이터를,
	 *  totalCount에 도달할 때까지 페이지를 넘겨가며 다 수..집? (한 번 요청 시 최대 값: 1000개)**/
	private List<FestivalApiRow> fetchAllRows() {
		FestivalApiResult firstPage = festivalPublicApiClient.fetch(1, MAX_PAGE_SIZE);
		int totalCount = firstPage.totalCount();

		List<FestivalApiRow> rows = new ArrayList<>(firstPage.rows());
		int fetched = firstPage.rows().size();
		int pIndex = 2;

		while (fetched < totalCount) {
			FestivalApiResult page = festivalPublicApiClient.fetch(pIndex, MAX_PAGE_SIZE);
			if (page.rows().isEmpty()) {
				break; // 안전장치: totalCount만큼 못 채워도 빈 페이지가 오면 무한루프 방지하고 중단
			}
			rows.addAll(page.rows());
			fetched += page.rows().size();
			pIndex++;
		}
		return rows;
	}

	/** 원본 스냅샷은 누적하지 않고 "최신 1건"만 유지 매번 새 row를 추가하면 거의 동일한 대용량 데이터가 배치 주기마다 중복 저장 **/
	private void saveRawSource(List<FestivalApiRow> rows) {
		try {
			String json = objectMapper.writeValueAsString(rows);
			publicFestivalSourceRepository.deleteAll();
			// 몇 건 받아왔는지 확인하려고 source(jsonb)를 매번 파싱하지 않도록, 건수를 별도 컬럼에 같이 저장
			publicFestivalSourceRepository.save(new PublicFestivalSource(json, rows.size()));
		} catch (Exception e) {
			// 원본 저장은 부가 기능이라, 실패해도 배치 본 로직(신규 행사 저장)까지 막으면 안 된다
			log.warn("원본 데이터 저장 실패, 배치는 계속 진행", e);
		}
	}

	/** yyyyMMdd 문자열이 주어진 연도로 시작하는지 확인. beginDe가 없는 경우(null)는 false로 제외 **/
	private boolean isInYear(String yyyyMMdd, int year) {
		return yyyyMMdd != null && yyyyMMdd.startsWith(String.valueOf(year));
	}

	// TODO: 서울 API 연동 시 toSeoulFestival() 추가
	/** 경기도 API 응답(Dto) -> Entity **/
	private Festival toGyeonggiFestival(FestivalApiRow row) {
		LocalDateTime beginDe = parseDate(row.beginDe());
		LocalDateTime endDe = parseDate(row.endDe());

		return Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.instNm(row.instNm())
			.title(row.title())
			.category(row.categoryNm())
			.url(row.url())
			.eventTmInfo(row.eventTmInfo())
			.partcptExpnInfo(row.partcptExpnInfo())
			.telnoInfo(row.telnoInfo())
			.hostInstNm(row.hostInstNm())
			.hmpgUrl(row.hmpgUrl())
			.imgUrl(row.imageUrl())
			.beginDe(beginDe)
			.endDe(endDe)
			.writngDe(parseDate(row.writngDe()))
			.status(resolveStatus(endDe))
			/** API 응답에 시/군 단위 지역 필드가 없어서, 일단 도 단위로만 저장 (경기도 전역 API) **/
			.region(FestivalRegion.GYEONGGI)
			.build();
	}

	/** 날짜 + 문자열 변환 + null 방어 **/
	private LocalDateTime parseDate(String yyyyMMdd) {
		if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
			return null;
		}
		return LocalDate.parse(yyyyMMdd, API_DATE_FORMAT).atStartOfDay();
	}

	private FestivalStatus resolveStatus(LocalDateTime endDe) {
		if (endDe == null) {
			return FestivalStatus.OPEN;
		}

		return endDe.isBefore(LocalDateTime.now())
				? FestivalStatus.CLOSED
				: FestivalStatus.OPEN;
	}
	/** 입력값의 앞뒤 공백을 제거하고, 빈 문자열은 검색 조건에서 제외한다.*/
	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim().toLowerCase(Locale.ROOT);
	}
}
