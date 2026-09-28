package com.team007.room_escape.domain.weather.infra.dto;

/** 단기예보(getVilageFcst) 응답 item 중 날씨 계산에 필요한 필드만. */
public record ShortTermForecastItem(
    String category, // 자료구분. "SKY"(하늘상태: 1맑음,3구름많음,4흐림), "PTY"(강수형태: 0없음,1비,2비/눈,3눈,4소나기), "POP"(강수확률 %) 등
    String fcstDate,  // 예보일자, yyyyMMdd
    String fcstTime,  // 예보시각, HHmm (예: 낮 12시 = "1200")
    String fcstValue  // 실제 값. category가 SKY/PTY면 코드 숫자, POP이면 강수확률(%) 숫자
) {
}
