package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalBrowseResponse;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.service.FestivalBrowseService;
import com.team007.room_escape.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalBrowseController {
    private final FestivalBrowseService festivalBrowseService;

    @Operation(summary = "회원 제보 행사 목록", description = "다른 회원을 포함한 전체 제보 행사의 공개 정보를 조회합니다. 개인 제보 내용은 포함하지 않습니다.")
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<Page<FestivalBrowseResponse>>> submissions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) FestivalStatus status,
            @PageableDefault(size = 6, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
        return ResponseEntity.ok(ApiResponse.success(festivalBrowseService.submissions(q, status, page)));
    }

    @Operation(summary = "행사 상세 조회", description = "문화행사 또는 회원 제보 행사의 공개 정보를 조회합니다.")
    @GetMapping("/{festivalId}")
    public ResponseEntity<ApiResponse<FestivalBrowseResponse>> detail(@PathVariable Long festivalId) {
        return ResponseEntity.ok(ApiResponse.success(festivalBrowseService.detail(festivalId)));
    }
}
