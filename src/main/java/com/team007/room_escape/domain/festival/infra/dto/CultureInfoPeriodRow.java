package com.team007.room_escape.domain.festival.infra.dto;

import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** /period2 응답의 item 1건. seq로 신규 여부만 판별하고, 상세 정보는 신규 건만 /detail2로 따로 받아온다. */
public record CultureInfoPeriodRow(
	@JacksonXmlProperty(localName = "seq") String seq,
	@JacksonXmlProperty(localName = "title") String title
) {
}
