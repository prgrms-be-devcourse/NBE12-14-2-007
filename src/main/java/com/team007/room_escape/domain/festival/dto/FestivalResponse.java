package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

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

    @Builder
    @Schema(description = "행사 상세 조회 응답")
    public record DetailResponse(
            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "데이터 출처", example = "MEMBER")
            ProviderType providerType,

            @Schema(description = "행사 제목")
            String title,

            @Schema(description = "행사 종류")
            String category,

            @Schema(description = "행사 소개")
            String festivalContent,

            @Schema(description = "기관명")
            String instNm,

            @Schema(description = "주최기관명")
            String hostInstNm,

            @Schema(description = "행사 이미지 URL")
            String imgUrl,

            @Schema(description = "행사 원문 주소")
            String url,

            @Schema(description = "행사 홈페이지 URL")
            String hmpgUrl,

            @Schema(description = "사용자에게 안내할 행사 참고 링크")
            String referenceUrl,

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

            @Schema(description = "행사 지역")
            FestivalRegion region,

            @Schema(description = "행사 상세 주소")
            String regionDetail,

            @Schema(description = "행사 상태", example = "OPEN")
            FestivalStatus status,

            @Schema(description = "정확해요 개수", example = "18")
            long accurateCount,

            @Schema(description = "부정확해요 개수", example = "2")
            long inaccurateCount,

            @Schema(description = "현재 사용자의 평가, 평가하지 않았다면 null", example = "ACCURATE", nullable = true)
            FestivalAccuracyVoteType myVote,

            @Schema(description = "좋아요 개수", example = "7")
            long likeCount,

            @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
            boolean likedByMe,

            @Schema(description = "회원 제보 행사 작성자 정보. 공공데이터 행사는 null", nullable = true)
            MemberResponse.MemberInfo member
    ) {

        public static DetailResponse from(Festival festival) {
            return from(festival, 0, 0, null, 0, false, null);
        }

        public static DetailResponse from(
                Festival festival,
                long accurateCount,
                long inaccurateCount,
                FestivalAccuracyVoteType myVote,
                long likeCount,
                boolean likedByMe,
                MemberResponse.MemberInfo member
        ) {
            return DetailResponse.builder()
                    .festivalId(festival.getId())
                    .providerType(festival.getProviderType())
                    .title(festival.getTitle())
                    .category(festival.getCategory())
                    .festivalContent(festival.getContent())
                    .instNm(festival.getInstNm())
                    .hostInstNm(festival.getHostInstNm())
                    .imgUrl(festival.getImgUrl())
                    .url(festival.getUrl())
                    .hmpgUrl(festival.getHmpgUrl())
                    .referenceUrl(festival.getHmpgUrl() != null && !festival.getHmpgUrl().isBlank()
                            ? festival.getHmpgUrl()
                            : festival.getUrl())
                    .beginDe(festival.getBeginDe())
                    .endDe(festival.getEndDe())
                    .eventTmInfo(festival.getEventTmInfo())
                    .partcptExpnInfo(festival.getPartcptExpnInfo())
                    .telnoInfo(festival.getTelnoInfo())
                    .region(festival.getRegion())
                    .regionDetail(festival.getRegionDetail())
                    .status(FestivalStatus.from(festival.getEndDe()))
                    .accurateCount(accurateCount)
                    .inaccurateCount(inaccurateCount)
                    .myVote(myVote)
                    .likeCount(likeCount)
                    .likedByMe(likedByMe)
                    .member(member)
                    .build();
        }
    }

    @Schema(description = "공공 행사 동기화 결과")
    public record SyncResponse(
            @Schema(description = "종료 처리(CLOSED)된 행사 목록")
            List<SyncedFestival> closedFestivals,

            @Schema(description = "새로 저장된 행사 목록")
            List<SyncedFestival> savedFestivals
    ) {
    }

    @Schema(description = "동기화된 행사 요약")
    public record SyncedFestival(
            @Schema(description = "행사 번호")
            Long festivalId,

            @Schema(description = "행사 제목")
            String title
    ) {

        public static SyncedFestival from(Festival festival) {
            return new SyncedFestival(festival.getId(), festival.getTitle());
        }
    }
}
