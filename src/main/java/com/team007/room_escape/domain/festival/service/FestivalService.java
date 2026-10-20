package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSearchRequest;
import com.team007.room_escape.domain.festival.infra.client.FestivalPublicApiClientV2;
import com.team007.room_escape.domain.festival.infra.dto.CultureInfoDetailRow;
import com.team007.room_escape.domain.festival.infra.dto.CultureInfoPeriodRow;
import com.team007.room_escape.domain.festival.infra.dto.FestivalVoteCount;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVote;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.entity.PublicFestivalSource;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.FestivalAccuracyVoteRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import com.team007.room_escape.domain.like.infra.dto.FestivalLikeCount;
import com.team007.room_escape.domain.like.infra.repository.LikeRepository;
import com.team007.room_escape.domain.like.type.LikeSort;
import com.team007.room_escape.global.config.R2Properties;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

	private static final int CULTURE_INFO_PAGE_SIZE = 9999;
	private static final int LEGACY_IMAGE_BATCH_SIZE = 30;

	private final FestivalRepository festivalRepository;
	private final FestivalAccuracyVoteRepository accuracyVoteRepository;
	private final LikeRepository likeRepository;
	private final PublicFestivalSourceRepository publicFestivalSourceRepository; /** 원본 저장용 **/
	private final FestivalPublicApiClientV2 festivalPublicApiClientV2;
	private final ObjectMapper objectMapper;
	private final ImageUrlResolver imageUrlResolver;
	private final FestivalImageProcessor festivalImageProcessor;
	private final R2Properties r2Properties;
	private final TransactionTemplate transactionTemplate;

	/** 오래 걸리는 API 호출·이미지 처리는 트랜잭션 밖에서 하고, DB 작업만 짧은 트랜잭션으로 커밋한다. */
	public FestivalResponse.SyncResponse syncPublicFestivals() {
		// 종료일이 지난 기존 행사를 먼저 CLOSED로 갱신한다.
		List<FestivalResponse.SyncedFestival> closedFestivals = transactionTemplate.execute(
			status -> closeExpiredFestivals());

		int currentYear = LocalDate.now().getYear();
		List<CultureInfoPeriodRow> allRows = fetchAllRows(currentYear + "0101", currentYear + "1231");

		// 원본 스냅샷 저장
		saveRawSource(allRows);

		// seq 존재 여부로 신규 행사를 정확히 판별한다 (건수 비교보다 API 응답 순서에 안전하다).
		List<String> seqs = allRows.stream().map(CultureInfoPeriodRow::seq).toList(); // 14,592건에서 seq 값들만 뽑아서 리스트로 만듦
		List<String> existingSeqs = festivalRepository.findExistingExternalIds(ProviderType.PUBLIC, seqs); //seq들 중 이미 DB에 저장된 것만 조회
		List<String> newSeqs = seqs.stream() //seqs에서 existingSeqs에 없는 것만 걸러냄 → 신규 행사
			.filter(seq -> !existingSeqs.contains(seq))
			.toList();

		if (newSeqs.isEmpty()) {
			log.info("신규 행사 없음 ({}년 전체 {}건, 신규 0건)", currentYear, allRows.size());
			migrateLegacyImages();
			return new FestivalResponse.SyncResponse(closedFestivals, List.of());
		}

		List<FestivalResponse.SyncedFestival> savedFestivals = List.of();
		// 원본 스냅샷은 이미 커밋돼서 여기서 실패해도 남는다.
		try {
			// 신규 건만 상세조회(가격 등 포함)까지 받아서 엔티티로 변환한다.
			List<Festival> festivals = newSeqs.stream()
				.map(festivalPublicApiClientV2::fetchDetail)
				.filter(Objects::nonNull) // 상세 조회가 빈 값으로 오는 경우 방어
				.map(this::toCultureInfoFestival) // 이미지 다운로드·업로드가 여기서 일어난다
				.toList();

			// 이미지 처리 후 저장만 한 트랜잭션으로 묶는다.
			festivalRepository.saveAll(festivals);
			// 저장 후에 만들어야 id가 채워져 있다.
			savedFestivals = festivals.stream().map(FestivalResponse.SyncedFestival::from).toList();
			log.info("공공 행사 {}건 저장 완료", savedFestivals.size());
		} catch (Exception e) {
			log.error("신규 행사 저장 실패, 원본 스냅샷은 반영됨", e);
		}
		migrateLegacyImages();
		return new FestivalResponse.SyncResponse(closedFestivals, savedFestivals);
	}

	/** 검색어와 선택 필터로 공공행사와 사용자 등록 행사를 통합 검색한다. */
	@Transactional(readOnly = true)
	public Page<FestivalResponse.ListItem> searchFestivals(
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

		if (LikeSort.isRequested(pageable)) {
			return toListItems(festivalRepository.searchFestivalsOrderByLikeCount(
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

					LikeSort.withoutSort(pageable)
			));
		}

		/** 정렬을 지정하지 않으면 오늘 기준으로 가장 가깝게 시작하는 행사부터 보여준다. */
		if (pageable.getSort().isUnsorted()) {
			return toListItems(festivalRepository.searchFestivalsOrderByNearestStart(
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

					LocalDate.now().atStartOfDay(),

					pageable
			));
		}

		return toListItems(festivalRepository.searchFestivals(
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
		));
	}

	/** 페이지에 담긴 행사들의 좋아요·투표 수를 한 번에 센다. (N+1 방지) */
	private Page<FestivalResponse.ListItem> toListItems(Page<Festival> festivals) {
		if (festivals.isEmpty()) {
			return festivals.map(festival -> FestivalResponse.ListItem.from(
				festival, 0L, 0L, 0L, imageUrlResolver));
		}

		List<Long> festivalIds = festivals.getContent().stream().map(Festival::getId).toList();
		Map<Long, ListCounts> counts = listCounts(festivalIds);

		return festivals.map(festival -> {
			ListCounts count = counts.get(festival.getId());
			return FestivalResponse.ListItem.from(
				festival,
				count.likeCount(),
				count.accurateCount(),
				count.inaccurateCount(),
				imageUrlResolver
			);
		});
	}

	/** 좋아요와 정확도 투표를 행사 번호 하나로 모은다. 없는 값은 0이다. */
	private Map<Long, ListCounts> listCounts(List<Long> festivalIds) {
		Map<Long, Long> likeCounts = likeRepository.countByFestivalIds(festivalIds).stream()
			.collect(Collectors.toMap(FestivalLikeCount::festivalId, FestivalLikeCount::likeCount));
		Map<Long, ListCounts> voteCounts = accuracyVoteRepository.countByFestivalIds(festivalIds).stream()
			.collect(Collectors.toMap(
				FestivalVoteCount::festivalId,
				ListCounts::fromVote,
				ListCounts::mergeVotes
			));

		return festivalIds.stream().collect(Collectors.toMap(
			id -> id,
			id -> voteCounts.getOrDefault(id, ListCounts.EMPTY)
				.withLike(likeCounts.getOrDefault(id, 0L))
		));
	}

	/** 행사 상세와 정확도 평가·좋아요 정보를 한 번에 조회한다. */
	@Transactional(readOnly = true)
	public FestivalResponse.Detail getFestival(Long festivalId, UUID memberId) {
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		long accurateCount = 0;
		long inaccurateCount = 0;
		FestivalAccuracyVoteType myVote = null;

		if (festival.getProviderType() == ProviderType.MEMBER) {
			accurateCount = accuracyVoteRepository.countByFestivalAndVoteType(
				festival,
				FestivalAccuracyVoteType.ACCURATE
			);
			inaccurateCount = accuracyVoteRepository.countByFestivalAndVoteType(
				festival,
				FestivalAccuracyVoteType.INACCURATE
			);

			if (memberId != null) {
				myVote = accuracyVoteRepository
					.findByFestivalIdAndMemberId(festivalId, memberId)
					.map(FestivalAccuracyVote::getVoteType)
					.orElse(null);
			}
		}

		long likeCount = likeRepository.countByFestivalId(festivalId);
		boolean likedByMe = memberId != null
			&& likeRepository.existsByFestivalIdAndMemberId(festivalId, memberId);
		return FestivalResponse.Detail.from(
			festival,
			accurateCount,
			inaccurateCount,
			myVote,
			likeCount,
			likedByMe,
			imageUrlResolver
		);
	}

	/** 종료일이 지난 OPEN 행사를 CLOSED로 갱신하고, 갱신된 목록을 돌려준다. */
	private List<FestivalResponse.SyncedFestival> closeExpiredFestivals() {
		LocalDateTime now = LocalDateTime.now();
		// 일괄 UPDATE는 바뀐 행을 돌려주지 않아서 갱신 전에 목록을 먼저 뽑는다.
		List<FestivalResponse.SyncedFestival> closed = festivalRepository.findByStatusAndEndDeBefore(FestivalStatus.OPEN, now).stream()
			.map(FestivalResponse.SyncedFestival::from)
			.toList();
		festivalRepository.closeExpiredFestivals(now);
		log.info("종료된 행사 {}건 CLOSED로 갱신", closed.size());
		return closed;
	}

	/** 받은 건수가 요청한 페이지 크기보다 적을 때까지(=마지막 페이지) 전부 받는다. */
	private List<CultureInfoPeriodRow> fetchAllRows(String from, String to) {
		List<CultureInfoPeriodRow> rows = new ArrayList<>();
		int pageNo = 1;
		while (true) {
			List<CultureInfoPeriodRow> page =
				festivalPublicApiClientV2.fetchPeriod(pageNo, CULTURE_INFO_PAGE_SIZE, from, to);
			if (page.isEmpty()) {
				break;
			}
			rows.addAll(page);
			if (page.size() < CULTURE_INFO_PAGE_SIZE) {
				break; // 요청한 개수보다 적게 왔으면 마지막 페이지
			}
			pageNo++;
		}
		return rows;
	}

	/** 원본 스냅샷은 누적하지 않고 최신 1건만 유지한다. */
	private void saveRawSource(List<CultureInfoPeriodRow> rows) {
		try {
			String json = objectMapper.writeValueAsString(rows);
			// 지우기와 저장을 한 트랜잭션으로 묶어 스냅샷이 비지 않게 한다.
			transactionTemplate.executeWithoutResult(status -> {
				publicFestivalSourceRepository.deleteAll();
				// jsonb를 파싱하지 않고 건수를 보려고 따로 저장한다.
				publicFestivalSourceRepository.save(new PublicFestivalSource(json, rows.size()));
			});
		} catch (Exception e) {
			// 원본 저장은 부가 기능이라 실패해도 배치를 막지 않는다.
			log.warn("원본 데이터 저장 실패, 배치는 계속 진행", e);
		}
	}

	/** R2로 옮겨지지 않은 기존 행사 이미지를 배치마다 조금씩 이관한다. 실패해도 동기화는 계속한다. */
	private void migrateLegacyImages() {
		try {
			Page<Festival> legacy = festivalRepository.findLegacyImages(
				r2Properties.publicUrl(), Pageable.ofSize(LEGACY_IMAGE_BATCH_SIZE));

			Map<Long, String> processedUrls = legacy.stream()
				.collect(Collectors.toMap(Festival::getId, f -> festivalImageProcessor.process(f.getImgUrl())));

			transactionTemplate.executeWithoutResult(status ->
				festivalRepository.findAllById(processedUrls.keySet())
					.forEach(f -> f.updateImgUrl(processedUrls.get(f.getId()))));
			log.info("기존 행사 이미지 {}건 이관 시도", processedUrls.size());
		} catch (Exception e) {
			log.warn("기존 이미지 이관 실패, 동기화는 계속 진행", e);
		}
	}

	/** 전국 문화정보 API 상세 행을 엔티티로 바꾼다. 이미지 다운로드·R2 업로드는 여기서 한다. */
	private Festival toCultureInfoFestival(CultureInfoDetailRow row) {
		return row.toFestival(festivalImageProcessor.process(row.imgUrl()));
	}

	/** 입력값의 앞뒤 공백을 제거하고, 빈 문자열은 검색 조건에서 제외한다. */
	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim().toLowerCase(Locale.ROOT);
	}

	/** 목록 한 줄에 붙일 좋아요·정확해요·부정확해요 개수. */
	private record ListCounts(long likeCount, long accurateCount, long inaccurateCount) {

		private static final ListCounts EMPTY = new ListCounts(0, 0, 0);

		private static ListCounts fromVote(FestivalVoteCount row) {
			return row.voteType() == FestivalAccuracyVoteType.ACCURATE
				? new ListCounts(0, row.count(), 0)
				: new ListCounts(0, 0, row.count());
		}

		private static ListCounts mergeVotes(ListCounts left, ListCounts right) {
			return new ListCounts(
				0,
				left.accurateCount() + right.accurateCount(),
				left.inaccurateCount() + right.inaccurateCount()
			);
		}

		private ListCounts withLike(long likeCount) {
			return new ListCounts(likeCount, accurateCount, inaccurateCount);
		}
	}
}
