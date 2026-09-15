package com.team007.room_escape.domain.festival.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Builder;

public class FestivalApplyRequest {

    private FestivalApplyRequest() {
    }

    @Builder
    @Schema(description = "민간행사 신청 요청")
    public record CreateFestivalApplyRequest(
            @Schema(description = "기관명", example = "방구석탈출")
            String instNm,

            @NotBlank(message = "행사 제목은 필수입니다.")
            @Schema(description = "행사 제목", example = "성수 독립 플리마켓")
            String title,

            @NotBlank(message = "카테고리는 필수입니다.")
            @Schema(description = "행사 카테고리", example = "플리마켓")
            String category,

            @NotBlank(message = "관리자 정보는 필수입니다.")
            @Schema(description = "행사 관리자", example = "홍길동")
            String manager,

            @NotBlank(message = "행사 내용은 필수입니다.")
            @Schema(description = "공개할 행사 상세 내용")
            String festivalContent,

            @NotBlank(message = "행사 주소는 필수입니다.")
            @Size(max = 2048, message = "주소는 2,048자 이하여야 합니다.")
            @Schema(description = "행사 주소", example = "서울특별시 성동구 성수동")
            String url,

            @Size(max = 2048, message = "이미지 URL은 2,048자 이하여야 합니다.")
            @Schema(description = "행사 이미지 URL")
            String imgUrl,

            @NotNull(message = "행사 시작 일시는 필수입니다.")
            @Schema(description = "행사 시작 일시", example = "2026-09-20T10:00:00")
            LocalDateTime beginDe,

            @NotNull(message = "행사 종료 일시는 필수입니다.")
            @Schema(description = "행사 종료 일시", example = "2026-09-20T18:00:00")
            LocalDateTime endDe,

            @NotBlank(message = "행사 시간 정보는 필수입니다.")
            @Schema(description = "행사 시간 정보", example = "10:00~18:00")
            String eventTmInfo,

            @Schema(description = "참가 비용 정보", example = "무료")
            String partcptExpnInfo,

            @Schema(description = "전화번호", example = "010-1234-5678")
            String telnoInfo,

            @Schema(description = "주최기관명", example = "방구석탈출")
            String hostInstNm,

            @Size(max = 2048, message = "홈페이지 URL은 2,048자 이하여야 합니다.")
            @Schema(description = "행사 홈페이지 URL")
            String hmpgUrl,

            @NotBlank(message = "행사 신청 내용은 필수입니다.")
            @Schema(description = "관리자에게 전달할 행사 신청 내용")
            String applyContent
    ) {

        @AssertTrue(message = "행사 종료 일시는 시작 일시보다 빠를 수 없습니다.")
        public boolean isValidPeriod() {
            return beginDe == null
                    || endDe == null
                    || !endDe.isBefore(beginDe);
        }
    }
}