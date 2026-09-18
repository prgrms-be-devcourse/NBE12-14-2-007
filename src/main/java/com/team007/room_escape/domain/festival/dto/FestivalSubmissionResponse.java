package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class FestivalSubmissionResponse {

    private FestivalSubmissionResponse() {
    }

    @Builder
    @Schema(description = "행사 등록 결과")
    public record CreateFestivalSubmissionResponse(
            @Schema(description = "생성된 행사 번호")
            Long festivalId,

            @Schema(description = "생성된 행사 제보 번호")
            UUID festivalSubmissionId
    ) {

        public static CreateFestivalSubmissionResponse from(
                Festival festival,
                FestivalSubmission festivalSubmission
        ) {
            return CreateFestivalSubmissionResponse.builder()
                    .festivalId(festival.getId())
                    .festivalSubmissionId(festivalSubmission.getId())
                    .build();
        }
    }

    @Builder
    @Schema(description = "사용자 등록 행사 목록 응답")
    public record FindAllFestivalSubmissionResponse(
            @Schema(description = "행사 제보 번호")
            UUID festivalSubmissionId,

            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 시작 일시")
            LocalDateTime beginDe,

            @Schema(description = "행사 종료 일시")
            LocalDateTime endDe,

            @Schema(description = "행사 제보 일시")
            LocalDateTime createdAt
    ) {

        public static FindAllFestivalSubmissionResponse from(FestivalSubmission festivalSubmission
        ) {
            Festival festival = festivalSubmission.getFestival();

            return FindAllFestivalSubmissionResponse.builder()
                    .festivalSubmissionId(festivalSubmission.getId())
                    .title(festival.getTitle())
                    .festivalId(festival.getId())
                    .beginDe(festival.getBeginDe())
                    .endDe(festival.getEndDe())
                    .createdAt(festivalSubmission.getCreatedAt())
                    .build();
        }
    }

    @Builder
    @Schema(description = "사용자 등록 행사 상세 응답")
    public record FindFestivalSubmissionResponse(
            @Schema(description = "행사 제보 정보")
            FestivalSubmissionDetail submission
    ) {

        public static FindFestivalSubmissionResponse from(
                FestivalSubmission festivalSubmission
        ) {
            Festival festival = festivalSubmission.getFestival();

            return FindFestivalSubmissionResponse.builder()
                    .submission(
                            FestivalSubmissionDetail.builder()
                                    .festivalSubmissionId(festivalSubmission.getId())
                                    .submissionContent(festivalSubmission.getContent())
                                    .createdAt(festivalSubmission.getCreatedAt())
                                    .updatedAt(festivalSubmission.getUpdatedAt())
                                    .festival(
                                            FestivalDetail.builder()
                                                    .festivalId(festival.getId())
                                                    .category(festival.getCategory())
                                                    .instNm(festival.getInstNm())
                                                    .title(festival.getTitle())
                                                    .manager(festival.getManager())
                                                    .festivalContent(festival.getContent())
                                                    .url(festival.getUrl())
                                                    .imgUrl(festival.getImgUrl())
                                                    .beginDe(festival.getBeginDe())
                                                    .endDe(festival.getEndDe())
                                                    .eventTmInfo(festival.getEventTmInfo())
                                                    .partcptExpnInfo(festival.getPartcptExpnInfo())
                                                    .telnoInfo(festival.getTelnoInfo())
                                                    .hostInstNm(festival.getHostInstNm())
                                                    .hmpgUrl(festival.getHmpgUrl())
                                                    .writngDe(festival.getWritngDe())
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
        }
    }

    @Builder
    @Schema(description = "행사 제보 상세 정보")
    public record FestivalSubmissionDetail(
            @Schema(description = "행사 제보 번호")
            UUID festivalSubmissionId,

            @Schema(description = "행사 제보 내용")
            String submissionContent,

            @Schema(description = "행사 제보 일시")
            LocalDateTime createdAt,

            @Schema(description = "마지막 수정 일시")
            LocalDateTime updatedAt,

            @Schema(description = "행사 정보")
            FestivalDetail festival
    ) {
    }

    @Builder
    @Schema(description = "행사 상세 정보")
    public record FestivalDetail(
            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "기관명")
            String instNm,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 종류")
            String category,

            @Schema(description = "행사 관리자")
            String manager,

            @Schema(description = "행사 상세 내용")
            String festivalContent,

            @Schema(description = "행사 주소")
            String url,

            @Schema(description = "행사 이미지 URL")
            String imgUrl,

            @Schema(description = "행사 시작 일시")
            LocalDateTime beginDe,

            @Schema(description = "행사 종료 일시")
            LocalDateTime endDe,

            @Schema(description = "행사 시간 정보")
            String eventTmInfo,

            @Schema(description = "참가 비용 정보")
            String partcptExpnInfo,

            @Schema(description = "전화번호")
            String telnoInfo,

            @Schema(description = "주최기관명")
            String hostInstNm,

            @Schema(description = "행사 홈페이지 URL")
            String hmpgUrl,

            @Schema(description = "행사 등록 일시")
            LocalDateTime writngDe
    ) {
    }
}
