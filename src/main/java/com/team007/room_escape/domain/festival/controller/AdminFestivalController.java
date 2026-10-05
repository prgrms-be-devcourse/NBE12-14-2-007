package com.team007.room_escape.domain.festival.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team007.room_escape.domain.festival.dto.AdminFestivalRequest;
import com.team007.room_escape.domain.festival.dto.AdminFestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.service.AdminFestivalService;
import com.team007.room_escape.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Festival", description = "관리자 행사 관리 API")
@RestController
@RequestMapping("/api/v1/admin/festivals")
@RequiredArgsConstructor
public class AdminFestivalController {

	private final AdminFestivalService adminFestivalService;

	@Operation(summary = "[ADMIN] 행사 목록 조회", description = "행사명·기관명·상세주소 검색어와 데이터 출처로 검색한다.")
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<AdminFestivalResponse.ListItem>>> search(
		@ParameterObject @ModelAttribute AdminFestivalRequest.Search request,
		@ParameterObject
		@PageableDefault(size = 20, sort = "beginDe", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		Page<AdminFestivalResponse.ListItem> response =
			adminFestivalService.search(request, pageable);

		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "[ADMIN] 행사 삭제", description = "삭제 시각만 남겨서 복구할 수 있다.")
	@DeleteMapping("/{festivalId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> delete(
		@PathVariable("festivalId") Long festivalId
	) {
		adminFestivalService.delete(festivalId);

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@Operation(summary = "[ADMIN] 행사 복구", description = "삭제된 공공 행사를 되살린다.")
	@PatchMapping("/{festivalId}/restore")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> restore(
		@PathVariable("festivalId") Long festivalId
	) {
		adminFestivalService.restore(festivalId);

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@Operation(summary = "[ADMIN] 행사 수정", description = "보낸 값으로 행사 정보를 전부 덮어쓴다.")
	@PatchMapping("/{festivalId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<FestivalResponse.Detail>> update(
		@PathVariable("festivalId") Long festivalId,
		@Valid @RequestBody AdminFestivalRequest.Update request
	) {
		FestivalResponse.Detail response =
			adminFestivalService.update(festivalId, request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
