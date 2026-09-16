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

	private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final FestivalRepository festivalRepository;
	private final PublicFestivalSourceRepository publicFestivalSourceRepository;
	private final FestivalPublicApiClient festivalPublicApiClient;

	@Transactional
	public void syncPublicFestivals() {
		/** 1건만 요청해서 전체 건수(totalCount)만 먼저 확인 **/
		int totalCount = festivalPublicApiClient.fetch(1, 1).totalCount();
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
