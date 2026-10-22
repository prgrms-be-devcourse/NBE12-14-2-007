package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.FestivalSubmission;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class FestivalSubmissionResponse {

	private FestivalSubmissionResponse() {
	}

	@Builder
	@Schema(name = "FestivalSubmissionCreate", description = "행사 등록 결과")
	public record Create(
		@Schema(description = "생성된 행사 번호")
		Long festivalId,

		@Schema(description = "생성된 행사 제보 번호")
		UUID festivalSubmissionId,

		@Schema(description = "행사 상태", example = "OPEN")
		FestivalStatus status
	) {

		public static Create from(Festival festival, FestivalSubmission festivalSubmission) {
			return Create.builder()
				.festivalId(festival.getId())
				.festivalSubmissionId(festivalSubmission.getId())
				.status(FestivalStatus.from(festival.getEndDe()))
				.build();
		}
	}

	@Builder
	@Schema(name = "FestivalSubmissionListItem", description = "[ME] 내 행사 목록 항목")
	public record ListItem(
		@Schema(description = "행사 제보 번호")
		UUID festivalSubmissionId,

		@Schema(description = "행사 번호")
		Long festivalId,

		@Schema(description = "행사 제목")
		String title,

		@Schema(description = "행사 지역")
		FestivalRegion region,

		@Schema(description = "행사 상세 주소")
		String regionDetail,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

		@Schema(description = "행사 제보 일시")
		LocalDateTime createdAt,

		@Schema(description = "삭제 정보. 삭제되지 않았으면 null")
		FestivalResponse.DeletionInfo deletion,

		@Schema(description = "행사 상태", example = "OPEN")
		FestivalStatus status
	) {

		public static ListItem from(FestivalSubmission festivalSubmission, DeletionHistory latest) {
			Festival festival = festivalSubmission.getFestival();

			return ListItem.builder()
				.festivalSubmissionId(festivalSubmission.getId())
				.festivalId(festival.getId())
				.title(festival.getTitle())
				.region(festival.getRegion())
				.regionDetail(festival.getRegionDetail())
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
				.createdAt(festivalSubmission.getCreatedAt())
				.deletion(FestivalResponse.DeletionInfo.from(festival, latest))
				.status(FestivalStatus.from(festival.getEndDe()))
				.build();
		}
	}

	@Builder
	@Schema(name = "FestivalSubmissionDetail", description = "[ME] 내 행사 상세")
	public record Detail(
		@Schema(description = "행사 제보 정보")
		SubmissionInfo submission
	) {

		public static Detail from(FestivalSubmission festivalSubmission, DeletionHistory latest) {
			return Detail.builder()
				.submission(SubmissionInfo.from(festivalSubmission, latest))
				.build();
		}
	}

	@Builder
	@Schema(name = "FestivalSubmissionInfo", description = "행사 제보 정보")
	public record SubmissionInfo(
		@Schema(description = "행사 제보 번호")
		UUID festivalSubmissionId,

		@Schema(description = "행사 제보 일시")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 일시")
		LocalDateTime updatedAt,

		@Schema(description = "행사 정보")
		FestivalInfo festival
	) {

		public static SubmissionInfo from(FestivalSubmission festivalSubmission, DeletionHistory latest) {
			return SubmissionInfo.builder()
				.festivalSubmissionId(festivalSubmission.getId())
				.createdAt(festivalSubmission.getCreatedAt())
				.updatedAt(festivalSubmission.getUpdatedAt())
				.festival(FestivalInfo.from(festivalSubmission.getFestival(), latest))
				.build();
		}
	}

	@Builder
	@Schema(name = "FestivalSubmissionFestivalInfo", description = "제보한 행사 정보")
	public record FestivalInfo(
		@Schema(description = "행사 번호")
		Long festivalId,

		@Schema(description = "행사 제목")
		String title,

		@Schema(description = "행사 종류")
		String category,

		@Schema(description = "행사 상세 내용")
		String festivalContent,

		@Schema(description = "행사 정보를 확인할 수 있는 참고 링크")
		String referenceUrl,

		@Schema(description = "행사 지역")
		FestivalRegion region,

		@Schema(description = "행사 상세 주소")
		String regionDetail,

		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

		@Schema(description = "참가 비용 정보")
		String partcptExpnInfo,

		@Schema(description = "전화번호")
		String telnoInfo,

		@Schema(description = "삭제 정보. 삭제되지 않았으면 null")
		FestivalResponse.DeletionInfo deletion,
		@Schema(description = "행사 상태", example = "OPEN")
		FestivalStatus status
	) {

		public static FestivalInfo from(Festival festival, DeletionHistory latest) {
			return FestivalInfo.builder()
				.festivalId(festival.getId())
				.category(festival.getCategory())
				.title(festival.getTitle())
				.festivalContent(festival.getContent())
				.referenceUrl(festival.getUrl())
				.region(festival.getRegion())
				.regionDetail(festival.getRegionDetail())
				.imgUrl(festival.getImgUrl())
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
				.partcptExpnInfo(festival.getPartcptExpnInfo())
				.telnoInfo(festival.getTelnoInfo())
				.deletion(FestivalResponse.DeletionInfo.from(festival, latest))
				.status(FestivalStatus.from(festival.getEndDe()))
				.build();
		}
	}
}
