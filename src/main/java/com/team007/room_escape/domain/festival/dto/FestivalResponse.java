package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.data.domain.Page;

public class FestivalResponse {

    private FestivalResponse() {
    }

    @Builder
    @Schema(description = "행사 목록 조회 응답")
    public record ListResponse(
            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "데이터 출처", example = "PUBLIC")
            ProviderType providerType,

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
                    .providerType(festival.getProviderType())
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

    // TODO: 다른 목록 API와 페이징 형식을 통일할 때 공통 PageResponse 도입 검토
    @Schema(description = "행사 검색 페이징 응답")
    public record PageResponse(
            List<ListResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {

        public static PageResponse from(Page<ListResponse> page) {
            return new PageResponse(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext()
            );
        }
    }
}
