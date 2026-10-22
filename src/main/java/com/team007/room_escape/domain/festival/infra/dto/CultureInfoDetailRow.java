package com.team007.room_escape.domain.festival.infra.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** /detail2 응답의 item 1건. */
public record CultureInfoDetailRow(
	@JacksonXmlProperty(localName = "seq") String seq,
	@JacksonXmlProperty(localName = "title") String title,
	@JacksonXmlProperty(localName = "startDate") String startDate,
	@JacksonXmlProperty(localName = "endDate") String endDate,
	@JacksonXmlProperty(localName = "place") String place,
	@JacksonXmlProperty(localName = "realmName") String realmName,
	@JacksonXmlProperty(localName = "area") String area,
	@JacksonXmlProperty(localName = "sigungu") String sigungu,
	@JacksonXmlProperty(localName = "price") String price,
	@JacksonXmlProperty(localName = "contents1") String contents1,
	@JacksonXmlProperty(localName = "url") String url,
	@JacksonXmlProperty(localName = "phone") String phone,
	@JacksonXmlProperty(localName = "imgUrl") String imgUrl,
	@JacksonXmlProperty(localName = "placeAddr") String placeAddr
) {

	private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	/** 전국 문화정보 API 행 1건을 공공 행사 엔티티로 바꾼다. 이미지는 서비스가 R2에 올린 뒤 URL만 넘겨준다. */
	public Festival toFestival(String processedImgUrl) {
		LocalDateTime begin = parseDate(startDate);
		LocalDateTime end = parseEndDate(endDate);

		return Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.externalId(seq)
			.title(title)
			.category(realmName)
			.content(contents1)
			.url(url)
			.telnoInfo(phone)
			.partcptExpnInfo(price)
			.imgUrl(processedImgUrl)
			.beginDe(begin)
			.endDe(end)
			.status(FestivalStatus.from(end))
			.region(FestivalRegion.fromDisplayName(area))
			.regionDetail(sigungu)
			.place(place)
			.placeAddress(placeAddr)
			.build();
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
