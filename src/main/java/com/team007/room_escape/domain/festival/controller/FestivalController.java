package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSearchRequest;
import com.team007.room_escape.domain.festival.service.FestivalService;
import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalController {

	private final FestivalService festivalService;

	@Operation(
			summary = "행사 통합 검색",
			description = """
                검색어와 지역, 데이터 출처, 행사 카테고리, 날짜를 이용해
                공공행사와 사용자 등록 행사를 통합 검색합니다.
                모든 검색 조건은 선택사항입니다.
                """
	)
	@GetMapping
	public ResponseEntity<ApiResponse<FestivalResponse.PageResponse>>
	searchFestivals(
			@ParameterObject
			@ModelAttribute FestivalSearchRequest request,
			@RequestParam(defaultValue = "0") int page
	) {
		FestivalResponse.PageResponse response =
				FestivalResponse.PageResponse.from(festivalService.searchFestivals(request, page));

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
