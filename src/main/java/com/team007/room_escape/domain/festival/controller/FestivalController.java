package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.service.FestivalService;
import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalController {

	private final FestivalService festivalService;

	@Operation(summary = "지역/날짜별 공공 행사 목록 조회", description = "사용자가 선택한 지역에서, 선택한 날짜에 진행 중인 공공 행사 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<FestivalResponse.ListResponse>>> getPublicFestivalsByRegion(
			@RequestParam FestivalRegion region,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@PageableDefault(sort = "beginDe", direction = Sort.Direction.ASC) Pageable page
	) {
		Page<FestivalResponse.ListResponse> response = festivalService.getPublicFestivalsByRegion(region, date, page);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "공공 행사 상세 조회", description = "공공 행사 1건의 상세 정보를 조회합니다.")
	@GetMapping("/{festivalId}")
	public ResponseEntity<ApiResponse<FestivalResponse.DetailResponse>> getPublicFestival(
			@PathVariable Long festivalId
	) {
		return ResponseEntity.ok(ApiResponse.success(festivalService.getPublicFestival(festivalId)));
	}
}
