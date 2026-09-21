package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

public class FestivalResponse {

    private FestivalResponse() {
    }

    @Builder
    @Schema(description = "행사 목록 조회 응답")
    public record ListResponse(
            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 종류")
            String category,

            @Schema(description = "기관명")
            String instNm,

            @Schema(description = "행사 이미지 URL")
            String imgUrl,

            @Schema(description = "행사 시작 일시")
            LocalDateTime beginDe,

            @Schema(description = "행사 종료 일시")
            LocalDateTime endDe,

            @Schema(description = "행사 지역")
            FestivalRegion region,

            @Schema(description = "행사 상태", example = "OPEN")
            FestivalStatus status
    ) {

        public static ListResponse from(Festival festival) {
            return ListResponse.builder()
                    .festivalId(festival.getId())
                    .title(festival.getTitle())
                    .category(festival.getCategory())
                    .instNm(festival.getInstNm())
                    .imgUrl(festival.getImgUrl())
                    .beginDe(festival.getBeginDe())
                    .endDe(festival.getEndDe())
                    .region(festival.getRegion())
                    .status(FestivalStatus.from(festival.getEndDe()))
                    .build();
        }
    }

    @Schema(description = "공공 행사 동기화 결과")
    public record SyncResponse(
            @Schema(description = "종료 처리(CLOSED)된 행사 건수")
            int closedCount,

            @Schema(description = "새로 저장된 행사 건수")
            int savedCount
    ) {
    }
}
