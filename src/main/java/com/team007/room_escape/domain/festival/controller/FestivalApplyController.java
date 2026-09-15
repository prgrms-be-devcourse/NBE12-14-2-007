package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalApplyRequest.CreateFestivalApplyRequest;
import com.team007.room_escape.domain.festival.dto.FestivalApplyResponse.CreateFestivalApplyResponse;
import com.team007.room_escape.domain.festival.dto.FestivalApplyResponse.FindAllFestivalApplyResponse;
import com.team007.room_escape.domain.festival.service.FestivalApplyService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FestivalApplyController {

    private final FestivalApplyService festivalApplyService;

    @PostMapping("/festival/submissions")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "민간행사 신청",
            description = "민간행사 정보와 행사 신청 내용을 등록합니다."
    )
    public ResponseEntity<ApiResponse<CreateFestivalApplyResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFestivalApplyRequest request
    ) {
        CreateFestivalApplyResponse response =
                festivalApplyService.create(principal.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/members/me/submissions")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "내 민간행사 신청 목록 조회",
            description = "로그인한 매니저가 신청한 민간행사 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<FindAllFestivalApplyResponse>>> findAllMine(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<FindAllFestivalApplyResponse> response =
                festivalApplyService.findAllByMemberId(principal.getId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}