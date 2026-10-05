package com.team007.room_escape.domain.festival.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// API 응답의 row(행사 1건)를 그대로 담는 DTO. 필드명을 API JSON에 맞춘다.
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
}
// API가 날짜를 "260915" 같은 문자열로 줘서 String으로 받고 나중에 파싱한다.