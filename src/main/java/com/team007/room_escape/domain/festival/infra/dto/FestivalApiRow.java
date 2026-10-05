package com.team007.room_escape.domain.festival.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// API 응답의 row(행사 1건)를 그대로 담는 DTO. 필드명을 API JSON에 맞춘다.
// API가 날짜를 "20260915" 같은 문자열로 줘서 String으로 받고 toFestival()에서 파싱한다.
public record FestivalApiRow(
	@JsonProperty("INST_NM") String instNm,
	@JsonProperty("TITLE") String title,
	@JsonProperty("CATEGORY_NM") String categoryNm,
	@JsonProperty("URL") String url,
	@JsonProperty("EVENT_TM_INFO") String eventTmInfo,
	@JsonProperty("PARTCPT_EXPN_INFO") String partcptExpnInfo,
	@JsonProperty("TELNO_INFO") String telnoInfo,
	@JsonProperty("HOST_INST_NM") String hostInstNm,
	@JsonProperty("HMPG_URL") String hmpgUrl,
	@JsonProperty("IMAGE_URL") String imageUrl,
	@JsonProperty("BEGIN_DE") String beginDe,
	@JsonProperty("END_DE") String endDe,
	@JsonProperty("WRITNG_DE") String writngDe
) {

	private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	/** 경기도 API 행 1건을 공공 행사 엔티티로 바꾼다. 이미지는 서비스가 R2에 올린 뒤 URL만 넘겨준다. */
	public Festival toFestival(String processedImgUrl) {
		LocalDateTime begin = parseDate(beginDe);
		LocalDateTime end = parseEndDate(endDe);

		return Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.instNm(instNm)
			.title(title)
			.category(categoryNm)
			.url(url)
			.eventTmInfo(eventTmInfo)
			.partcptExpnInfo(partcptExpnInfo)
			.telnoInfo(telnoInfo)
			.hostInstNm(hostInstNm)
			.hmpgUrl(normalizeHomepageUrl(hmpgUrl))
			.imgUrl(processedImgUrl)
			.beginDe(begin)
			.endDe(end)
			.writngDe(parseDate(writngDe))
			.status(FestivalStatus.from(end))
			// 시/군 단위 지역 필드가 없어서 도 단위로만 저장한다.
			.region(FestivalRegion.GYEONGGI)
			.build();
	}

	/** 스킴 없이 오는 HMPG_URL에 https://를 붙이고, 앞에 섞인 문자는 잘라낸다. */
	private static String normalizeHomepageUrl(String value) {
		if (value == null || value.isBlank()
				|| value.equals("-") || value.equalsIgnoreCase("undefined")) {
			return null;
		}
		String trimmed = value.trim();
		int httpIndex = trimmed.toLowerCase().indexOf("http://");
		if (httpIndex < 0) {
			httpIndex = trimmed.toLowerCase().indexOf("https://");
		}
		if (httpIndex >= 0) {
			return trimmed.substring(httpIndex);
		}
		return "https://" + trimmed;
	}

	/** yyyyMMdd 문자열을 날짜로 바꾼다. 비어 있으면 null. */
	private static LocalDateTime parseDate(String yyyyMMdd) {
		if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
			return null;
		}
		return LocalDate.parse(yyyyMMdd, API_DATE_FORMAT).atStartOfDay();
	}

	/** 종료일은 그날 23:59:59까지로 해석한다. 마지막 날 자정에 종료 처리되는 걸 막는다. */
	private static LocalDateTime parseEndDate(String yyyyMMdd) {
		if (yyyyMMdd == null || yyyyMMdd.isBlank()) {
			return null;
		}
		return LocalDate.parse(yyyyMMdd, API_DATE_FORMAT).atTime(LocalTime.MAX);
	}
}
