package com.team007.room_escape.domain.festival.dto;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionAction;
import com.team007.room_escape.domain.deletion.infra.entity.DeletionActorType;
import com.team007.room_escape.domain.festival.infra.entity.FestivalAccuracyVoteType;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public class FestivalResponse {

	@Builder
	@Schema(name = "FestivalDeletionInfo", description = "현재 삭제 상태. 삭제되지 않았으면 null")
	public record DeletionInfo(
		@Schema(description = "행사 삭제 시각") LocalDateTime deletedAt,
		@Schema(description = "이력에 기록된 삭제 주체. AUTHOR, ADMIN. 이력이 없으면 null") DeletionActorType type,
		@Schema(description = "작성자에게 안내하는 삭제 사유") String reason
	) {
		public static DeletionInfo from(Festival festival, DeletionHistory latest) {
			if (!festival.isDeleted()) {
				return null;
			}
			if (latest == null || latest.getAction() != DeletionAction.DELETE) {
				return new DeletionInfo(festival.getDeletedAt(), null, "삭제 사유를 확인할 수 없습니다.");
			}
			String reason = latest.getReason().getDescription();
			if (latest.getReasonDetail() != null) {
				reason += " — " + latest.getReasonDetail();
			}
			return new DeletionInfo(festival.getDeletedAt(),
				latest.getActorType(),
				reason);
		}
	}

	private FestivalResponse() {
	}

	@Builder
	@Schema(name = "FestivalListItem", description = "행사 목록 항목")
	public record ListItem(
		@Schema(description = "행사 번호")
		Long festivalId,

		@Schema(description = "데이터 출처", example = "PUBLIC")
		ProviderType providerType,

		@Schema(description = "행사 제목")
		String title,

		@Schema(description = "행사 종류")
		String category,

		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

		@Schema(description = "행사 지역")
		FestivalRegion region,

		@Schema(description = "행사 지역 상세(시·군·구)", nullable = true)
		String regionDetail,

		@Schema(description = "행사 상태", example = "OPEN")
		FestivalStatus status,

		@Schema(description = "좋아요 수")
		long likeCount,

		@Schema(description = "정확해요 개수. 공공데이터 행사는 0", example = "3")
		long accurateCount,

		@Schema(description = "부정확해요 개수. 공공데이터 행사는 0", example = "1")
		long inaccurateCount,

		@Schema(description = "회원 제보 행사 작성자 정보. 공공데이터 행사는 null", nullable = true)
		MemberResponse.MemberInfo member
	) {

		public static ListItem from(
			Festival festival,
			long likeCount,
			long accurateCount,
			long inaccurateCount,
			ImageUrlResolver imageUrlResolver
		) {
			return ListItem.builder()
				.festivalId(festival.getId())
				.providerType(festival.getProviderType())
				.title(festival.getTitle())
				.category(festival.getCategory())
				.imgUrl(image(festival.getImgUrl(), imageUrlResolver))
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
				.region(festival.getRegion())
				.regionDetail(festival.getRegionDetail())
				.status(FestivalStatus.from(festival.getEndDe()))
				.likeCount(likeCount)
				.accurateCount(accurateCount)
				.inaccurateCount(inaccurateCount)
				.member(toMember(festival, imageUrlResolver))
				.build();
		}
	}

	@Builder
	@Schema(name = "FestivalDetail", description = "행사 상세")
	public record Detail(
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

		@Schema(description = "행사 이미지 URL")
		String imgUrl,

		@Schema(description = "행사 원문 주소")
		String url,

		@Schema(description = "사용자에게 안내할 행사 참고 링크")
		String referenceUrl,

		@Schema(description = "행사 시작 일시")
		LocalDateTime beginDe,

		@Schema(description = "행사 종료 일시")
		LocalDateTime endDe,

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

		public static Detail from(
			Festival festival,
			long accurateCount,
			long inaccurateCount,
			FestivalAccuracyVoteType myVote,
			long likeCount,
			boolean likedByMe,
			ImageUrlResolver imageUrlResolver
		) {
			return Detail.builder()
				.festivalId(festival.getId())
				.providerType(festival.getProviderType())
				.title(festival.getTitle())
				.category(festival.getCategory())
				.festivalContent(festival.getContent())
				.imgUrl(image(festival.getImgUrl(), imageUrlResolver))
				.url(festival.getUrl())
				.referenceUrl(festival.getUrl())
				.beginDe(festival.getBeginDe())
				.endDe(festival.getEndDe())
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
				.member(toMember(festival, imageUrlResolver))
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

	/** 회원 제보 행사의 제보자. 공공데이터 행사는 제보자가 없어 null이다. */
	private static MemberResponse.MemberInfo toMember(Festival festival, ImageUrlResolver imageUrlResolver) {
		Member member = festival.getMember();
		if (member == null || imageUrlResolver == null) {
			return null;
		}
		return MemberResponse.MemberInfo.from(member, imageUrlResolver);
	}

	private static String image(String value, ImageUrlResolver imageUrlResolver) {
		return imageUrlResolver == null ? value : imageUrlResolver.resolve(value);
	}
}
