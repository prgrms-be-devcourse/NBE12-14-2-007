package com.team007.room_escape.domain.deletion.controller;

import com.team007.room_escape.domain.deletion.dto.AdminDeletionRequest;
import com.team007.room_escape.domain.deletion.dto.DeletionHistoryResponse;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionTargetType;
import com.team007.room_escape.domain.deletion.service.AdminDeletionFacade;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/deletions")
@RequiredArgsConstructor
public class AdminDeletionController {
	private final AdminDeletionFacade facade;

	@GetMapping("/latest")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "[ADMIN] 최신 삭제·복구 이력 조회", description = "기록이 없으면 data는 null입니다. 현재 삭제 상태는 콘텐츠 조회 결과로 확인하며 최종 복구 가능 여부는 복구 요청 시 다시 검증합니다.")
	public ResponseEntity<ApiResponse<DeletionHistoryResponse.Latest>> latest(
		@AuthenticationPrincipal CustomUserDetails principal,
		@RequestParam DeletionTargetType targetType,
		@RequestParam String targetId
	) {
		return ResponseEntity.ok(ApiResponse.success(facade.latest(principal.getId(), targetType, targetId)));
	}

	@PostMapping("/restore")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "[ADMIN] 콘텐츠 복구", description = "행사·후기·후기 댓글·커뮤니티 글/댓글을 복구합니다. 최근 조치가 관리자 삭제인 경우만 허용하며 댓글은 부모 글이 게시 중이어야 합니다. 기존 이력은 보존합니다.")
	public ResponseEntity<ApiResponse<Void>> restore(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody AdminDeletionRequest.Restore request
	) {
		facade.restore(principal.getId(), request.targetType(), request.targetId());
		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "[ADMIN] 콘텐츠 삭제", description = "삭제와 사유 이력을 함께 저장합니다. inquiryId는 선택이며 지정하면 동일 대상의 미처리 신고만 처리 완료로 변경합니다. 연결 실패 시 삭제도 취소됩니다.")
	public ResponseEntity<ApiResponse<Void>> delete(
		@AuthenticationPrincipal CustomUserDetails principal,
		@Valid @RequestBody AdminDeletionRequest.Create request
	) {
		facade.delete(principal.getId(), request);
		return ResponseEntity.ok(ApiResponse.noContentSuccess());
	}
}
