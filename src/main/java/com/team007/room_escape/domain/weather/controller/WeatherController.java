package com.team007.room_escape.domain.weather.controller;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.weather.dto.WeatherResponse;
import com.team007.room_escape.domain.weather.service.WeatherService;
import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

	private final WeatherService weatherService;

	@Operation(summary = "지역·날짜별 날씨 조회",
		description = "단기예보 제공 범위(오늘~2일 후) 밖이거나 조회에 실패하면 UNKNOWN을 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
		@RequestParam FestivalRegion region,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		WeatherResponse response = weatherService.getWeather(region, date);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
