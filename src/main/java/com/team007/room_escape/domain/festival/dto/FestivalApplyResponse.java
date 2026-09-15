package com.team007.room_escape.domain.festival.dto;


import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApply;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class FestivalApplyResponse {

    private FestivalApplyResponse() {
    }

    @Builder
    @Schema(description = "민간행사 신청 결과")
    public record CreateFestivalApplyResponse(
            @Schema(description = "생성된 행사 번호")
            Long festivalId,

            @Schema(description = "생성된 행사 신청 번호")
            UUID festivalApplyId,

            @Schema(description = "행사 신청 검수 상태")
            FestivalApplyStatus applyStatus
    ) {

        public static CreateFestivalApplyResponse from(
                Festival festival,
                FestivalApply festivalApply
        ) {
            return CreateFestivalApplyResponse.builder()
                    .festivalId(festival.getId())
                    .festivalApplyId(festivalApply.getId())
                    .applyStatus(festival.getApplyStatus())
                    .build();
        }
    }

    @Builder
    @Schema(description = "민간행사 신청 목록 응답")
    public record FindAllFestivalApplyResponse(
            @Schema(description = "행사 신청 번호")
            UUID festivalApplyId,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 시작 일시")
            LocalDateTime beginDe,

            @Schema(description = "행사 종료 일시")
            LocalDateTime endDe,

            @Schema(description = "행사 신청 검수 상태")
            FestivalApplyStatus applyStatus,

            @Schema(description = "행사 신청 일시")
            LocalDateTime writngDe
    ) {

        public static FindAllFestivalApplyResponse from(
                Festival festival,
                FestivalApply festivalApply
        ) {
            return FindAllFestivalApplyResponse.builder()
                    .festivalApplyId(festivalApply.getId())
                    .title(festival.getTitle())
                    .beginDe(festival.getBeginDe())
                    .endDe(festival.getEndDe())
                    .applyStatus(festival.getApplyStatus())
                    .writngDe(festival.getWritngDe())
                    .build();
        }
    }
}
