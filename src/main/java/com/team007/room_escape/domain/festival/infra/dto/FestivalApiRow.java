package com.team007.room_escape.domain.festival.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

//API 응답의 row(행사 1건)를 그대로 담는 DTO
//API가 주는 Json 필드 자바 필드와 맞춰 매칭하게끔
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
//날짜도 String인 이유 : API가 날짜 타입이 아니라
//260915 이런 식으로 문자열 형식으로 줌 => 파싱은 나중