package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalApplyRequest.CreateFestivalApplyRequest;
import com.team007.room_escape.domain.festival.dto.FestivalApplyResponse.CreateFestivalApplyResponse;
import com.team007.room_escape.domain.festival.service.FestivalApplyService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/festival/submissions")
@RequiredArgsConstructor
public class FestivalApplyController {

    private final FestivalApplyService festivalApplyService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "민간행사 신청", description = "민간행사 정보와 행사 신청 내용을 등록합니다."
    )
    public ResponseEntity<ApiResponse<CreateFestivalApplyResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFestivalApplyRequest request) {
        CreateFestivalApplyResponse response =
                festivalApplyService.create(principal.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}