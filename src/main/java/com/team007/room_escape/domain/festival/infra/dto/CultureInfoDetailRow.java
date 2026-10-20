package com.team007.room_escape.domain.festival.infra.dto;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** /detail2 응답의 item 1건. Festival로 변환하는 메서드는 FestivalRegion.fromDisplayName()이 준비되면 추가한다. */
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
}
