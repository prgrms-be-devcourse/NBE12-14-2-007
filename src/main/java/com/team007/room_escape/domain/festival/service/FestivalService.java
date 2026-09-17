package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.infra.client.FestivalPublicApiClient;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

	/** API가 날짜열"260916"형태로 넘겨줘서 해석하는 규칙**/
	private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final FestivalRepository festivalRepository;
	private final PublicFestivalSourceRepository publicFestivalSourceRepository; /** 원본 저장용 **/
	private final FestivalPublicApiClient festivalPublicApiClient;

	@Transactional
	public void syncPublicFestivals() {
		/** fetch(1, 1) => 1건만 요청해서 전체 건수(totalCount)만 먼저 확인
		 *  1건을 요청하면 head.list_total_count가 같이 오니까 이걸 확인하기 위해 1건을 불러온다 **/
		int totalCount = festivalPublicApiClient.fetch(1, 1).totalCount();

		/** 우리 DB에 저장된 PUBLIC 타입 건수 조회 **/
		long dbCount = festivalRepository.countByProviderType(ProviderType.PUBLIC);

		int n = (int) (totalCount - dbCount);
		if (n <= 0) {
			log.info("신규 행사 없음 (전체 {}건, 저장된 {}건)", totalCount, dbCount);
			return;
		}

		List<FestivalApiRow> rows = festivalPublicApiClient.fetch(1, n).rows();
		List<Festival> festivals = rows.stream()
			.map(this::toFestival)
			.toList();

		festivalRepository.saveAll(festivals);
		log.info("공공 행사 {}건 저장 완료", festivals.size());
	}

	/** Dto -> Entity **/
	private Festival toFestival(FestivalApiRow row) {
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
		return endDe.isBefore(LocalDateTime.now()) ? FestivalStatus.CLOSED : FestivalStatus.OPEN;
	}
}
