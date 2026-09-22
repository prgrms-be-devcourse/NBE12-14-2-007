package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSearchRequest;
import com.team007.room_escape.domain.festival.service.FestivalService;
import com.team007.room_escape.domain.festival.service.FestivalSyncExecutor;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalController {

	private final FestivalService festivalService;
	private final FestivalSyncExecutor festivalSyncExecutor;

	@Operation(summary = "행사 통합 검색", description = """
                검색어와 지역, 데이터 출처, 행사 카테고리, 날짜를 이용해
                공공행사와 사용자 등록 행사를 통합 검색합니다.
                모든 검색 조건은 선택사항입니다""")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<FestivalResponse.ListResponse>>>
	searchFestivals(
			@ParameterObject
			@ModelAttribute FestivalSearchRequest request,
			@ParameterObject
			@PageableDefault(size = 9, sort = "beginDe", direction = Sort.Direction.ASC)
			Pageable pageable
	) {
		Page<FestivalResponse.ListResponse> response =
				festivalService.searchFestivals(request, pageable);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "공공 행사 상세 조회", description = "공공 행사 1건의 상세 정보를 조회합니다.")
	@GetMapping("/{festivalId}")
	public ResponseEntity<ApiResponse<FestivalResponse.DetailResponse>> getPublicFestival(
			@PathVariable Long festivalId
	) {
		FestivalResponse.DetailResponse response = festivalService.getPublicFestival(festivalId);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "공공 행사 수동 동기화",
		description = "관리자가 공공 API 데이터를 즉시 동기화하고, 끝나면 결과를 반환합니다. 이미 진행 중이면 409를 반환합니다.")
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/sync")
	public ResponseEntity<ApiResponse<FestivalResponse.SyncResponse>> syncPublicFestivals() {
		FestivalResponse.SyncResponse result = festivalSyncExecutor.tryRun()
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.SYNC_ALREADY_RUNNING));

		return ResponseEntity.ok(ApiResponse.success(result));
	}
}
