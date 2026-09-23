package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteRequest.CreateOrUpdateFestivalAccuracyVoteRequest;
import com.team007.room_escape.domain.festival.dto.FestivalAccuracyVoteResponse.AccuracyVoteResponse;
import com.team007.room_escape.domain.festival.service.FestivalAccuracyVoteService;
import com.team007.room_escape.global.response.ApiResponse;
import com.team007.room_escape.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals/{festivalId}/accuracy-votes")
@RequiredArgsConstructor
public class FestivalAccuracyVoteController {

    private final FestivalAccuracyVoteService accuracyVoteService;

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "행사 정보 정확도 평가",
            description = "사용자 등록 행사에 정확해요 또는 정보가 달라요 평가를 등록하거나 변경합니다."
    )
    public ResponseEntity<ApiResponse<AccuracyVoteResponse>> vote(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long festivalId,
            @Valid @RequestBody CreateOrUpdateFestivalAccuracyVoteRequest request
    ) {
        AccuracyVoteResponse response = accuracyVoteService.vote(
                principal.getId(),
                festivalId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "행사 정보 정확도 평가 취소",
            description = "로그인한 사용자의 행사 정보 정확도 평가를 취소합니다."
    )
    public ResponseEntity<ApiResponse<AccuracyVoteResponse>> cancelVote(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long festivalId
    ) {
        AccuracyVoteResponse response = accuracyVoteService.cancelVote(
                principal.getId(),
                festivalId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
