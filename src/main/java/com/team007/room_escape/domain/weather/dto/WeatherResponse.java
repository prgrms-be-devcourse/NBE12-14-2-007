package com.team007.room_escape.domain.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "지역·날짜별 간단 날씨")
public record WeatherResponse(

    @Schema(description = "날씨 상태", example = "SUNNY")
    WeatherCondition condition,

    @Schema(description = "강수확률(%). 값이 없으면 null", example = "30")
    Integer precipitationProbability,

    @Schema(description = "조회한 날짜", example = "2026-09-25")
    LocalDate date
) {
    public static WeatherResponse unknown(LocalDate date) {
        return new WeatherResponse(WeatherCondition.UNKNOWN, null, date);
    }
}
