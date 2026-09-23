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

	@Operation(
		summary = "[ADMIN] 행사 목록 조회",
		description = """
			행사명·기관명·상세주소 검색어와 데이터 출처로 검색한다. 조건을 비우면 전체를 조회한다.
			includeDeleted=true 면 삭제된 행사도 함께 조회한다. 복구 대상을 찾으려면 필요하다.
			기본 정렬은 시작일 최신순이며, sort 로 endDe, title, createdAt 도 쓸 수 있다.
			"""
	)
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

	@Operation(
		summary = "[ADMIN] 행사 삭제",
		description = """
			행사를 삭제한다. 행을 지우지 않고 삭제 시각만 남기므로 복구할 수 있다.
			이 행사에 달린 후기는 지워지지 않는다.
			"""
	)
	@DeleteMapping("/{festivalId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> delete(
		@PathVariable("festivalId") Long festivalId
	) {
		adminFestivalService.delete(festivalId);

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@Operation(
		summary = "[ADMIN] 행사 복구",
		description = """
			삭제된 행사를 되살린다.
			삭제되지 않은 행사면 409(FESTIVAL007)로 거부한다.
			회원이 제보한 행사는 작성자가 직접 지웠을 수 있어 403(FESTIVAL008)으로 거부한다.
			"""
	)
	@PatchMapping("/{festivalId}/restore")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> restore(
		@PathVariable("festivalId") Long festivalId
	) {
		adminFestivalService.restore(festivalId);

		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@Operation(
		summary = "[ADMIN] 행사 수정",
		description = """
			행사 정보를 수정한다. 공공 행사와 회원 제보를 모두 고칠 수 있다.
			보낸 값으로 전부 덮어쓰므로 상세 조회로 현재 값을 먼저 받아서 채워 보낸다.
			데이터 출처(providerType)와 작성자는 바꿀 수 없다.
			진행 상태는 종료일로 다시 계산되므로 따로 보내지 않는다.
			종료일이 시작일보다 앞서면 400으로 거부한다.
			"""
	)
	@PatchMapping("/{festivalId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<FestivalResponse.DetailResponse>> update(
		@PathVariable("festivalId") Long festivalId,
		@Valid @RequestBody AdminFestivalRequest.Update request
	) {
		FestivalResponse.DetailResponse response =
			adminFestivalService.update(festivalId, request);

		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
