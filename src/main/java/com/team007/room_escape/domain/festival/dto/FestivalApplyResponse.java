package com.team007.room_escape.domain.festival.dto;


import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApply;
import com.team007.room_escape.domain.festival.infra.entity.FestivalApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
}