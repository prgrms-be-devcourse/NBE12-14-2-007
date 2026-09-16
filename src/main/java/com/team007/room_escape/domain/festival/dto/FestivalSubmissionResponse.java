package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApplyStatus;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class FestivalSubmissionResponse {

    private FestivalSubmissionResponse() {
    }

    @Builder
    @Schema(description = "행사 제보 결과")
    public record CreateFestivalSubmissionResponse(
            @Schema(description = "생성된 행사 번호")
            Long festivalId,

            @Schema(description = "생성된 행사 제보 번호")
            UUID festivalSubmissionId,

            @Schema(description = "행사 제보 상태")
            FestivalApplyStatus applyStatus
    ) {

        public static CreateFestivalSubmissionResponse from(
                Festival festival,
                FestivalSubmission festivalSubmission
        ) {
            return CreateFestivalSubmissionResponse.builder()
                    .festivalId(festival.getId())
                    .festivalSubmissionId(festivalSubmission.getId())
                    .applyStatus(festival.getApplyStatus())
                    .build();
        }
    }

    @Builder
    @Schema(description = "행사 제보 목록 응답")
    public record FindAllFestivalSubmissionResponse(
            @Schema(description = "행사 제보 번호")
            UUID festivalSubmissionId,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 시작 일시")
            LocalDateTime beginDe,

            @Schema(description = "행사 종료 일시")
            LocalDateTime endDe,

            @Schema(description = "행사 제보 상태")
            FestivalApplyStatus applyStatus,

            @Schema(description = "행사 제보 일시")
            LocalDateTime writngDe
    ) {

        public static FindAllFestivalSubmissionResponse from(FestivalSubmission festivalSubmission
        ) {
            Festival festival = festivalSubmission.getFestival();

            return FindAllFestivalSubmissionResponse.builder()
                    .festivalSubmissionId(festivalSubmission.getId())
                    .title(festival.getTitle())
                    .beginDe(festival.getBeginDe())
                    .endDe(festival.getEndDe())
                    .applyStatus(festival.getApplyStatus())
                    .writngDe(festival.getWritngDe())
                    .build();
        }
    }
}
