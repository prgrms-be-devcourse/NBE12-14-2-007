package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalSubmissionRequest.CreateFestivalSubmissionRequest;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.CreateFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindAllFestivalSubmissionResponse;
import com.team007.room_escape.domain.festival.service.FestivalSubmissionService;
import com.team007.room_escape.domain.festival.dto.FestivalSubmissionResponse.FindFestivalSubmissionResponse;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FestivalSubmissionController {

    private final FestivalSubmissionService festivalSubmissionService;

    @PostMapping("/festivals/submissions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "행사 정보 등록",
            description = "로그인한 사용자가 행사 내용을 제보합니다."
    )
    public ResponseEntity<ApiResponse<CreateFestivalSubmissionResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFestivalSubmissionRequest request
    ) {
        CreateFestivalSubmissionResponse response =
                festivalSubmissionService.create(principal.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/members/me/submissions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사용자가 등록한 행사 목록 조회",
            description = "로그인한 사용자가 제보한 행사 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<FindAllFestivalSubmissionResponse>>> findAllMine(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        List<FindAllFestivalSubmissionResponse> response =
                festivalSubmissionService.findAllByMemberId(principal.getId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/members/me/submissions/{submissionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사용자가 등록한 행사 상세 조회",
            description = "로그인한 사용자가 자신이 제보한 행사 정보를 상세 조회합니다."
    )
    public ResponseEntity<ApiResponse<FindFestivalSubmissionResponse>> findMine(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID submissionId
    ) {
        FindFestivalSubmissionResponse response =
                festivalSubmissionService.findById(
                        principal.getId(),
                        submissionId
                );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
