package com.team007.room_escape.domain.festival.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.festival.dto.AdminFestivalRequest;
import com.team007.room_escape.domain.festival.dto.AdminFestivalResponse;
import com.team007.room_escape.domain.festival.dto.FestivalResponse;
import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.service.MemberTrustGradeService;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFestivalService {

	/**
	 * 정렬에 허용하는 필드.
	 * Pageable 의 sort 는 클라이언트가 아무 이름이나 넣을 수 있어서,
	 * 엔티티에 없는 필드가 들어오면 500이 난다. 그래서 먼저 걸러낸다.
	 */
	private static final Set<String> SORTABLE =
		Set.of("beginDe", "endDe", "title", "createdAt");

	private final FestivalRepository festivalRepository;
	private final MemberTrustGradeService memberTrustGradeService;

	/**
	 * 관리자 행사 검색. 조건을 비우면 전체를 조회한다.
	 * 공개 검색과 달리 삭제된 행사까지 볼 수 있다. 그래야 복구 대상을 찾을 수 있다.
	 */
	@Transactional(readOnly = true)
	public Page<AdminFestivalResponse.ListItem> search(
		AdminFestivalRequest.Search request,
		Pageable pageable
	) {
		validateSort(pageable.getSort());

		return festivalRepository.searchForAdmin(
			request.keywordOrEmpty(),
			request.providerType(),
			request.includeDeletedOrFalse(),
			request.excludeClosedOrFalse(),
			pageable
		).map(AdminFestivalResponse.ListItem::from);
	}

	/**
	 * 행사를 삭제한다. 행을 지우지 않고 삭제 시각만 남기므로 되돌릴 수 있다.
	 *
	 * 이 행사에 달린 후기는 그대로 남는다. Festival 에 @SQLRestriction 이 없어서
	 * 후기 조회 시 연관관계 로딩은 계속 되지만, 행사 검색 결과에서는 사라진다.
	 */
	@Transactional
	public void delete(Long festivalId) {
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		festival.delete();
		festivalRepository.flush();
		memberTrustGradeService.refreshForFestival(festival);
	}

	/**
	 * 삭제된 행사를 되살린다.
	 *
	 * 회원 제보는 작성자가 직접 지웠을 수도 있는데 deletedAt 하나로는 누가 지웠는지
	 * 구분할 수 없다. 관리자가 남의 글을 임의로 되살리는 일을 막기 위해
	 * 지금은 공공 행사만 복구할 수 있게 한다.
	 *
	 * TODO 삭제 주체를 기록하게 되면 회원 제보도 "관리자가 지운 것"만 복구를 열 것.
	 */
	@Transactional
	public void restore(Long festivalId) {
		Festival festival = festivalRepository.findById(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		if (!festival.isDeleted()) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_DELETED);
		}
		if (festival.getProviderType() != ProviderType.PUBLIC) {
			throw new BusinessException(FestivalExceptionCode.FESTIVAL_RESTORE_FORBIDDEN);
		}

		festival.restore();
	}

	private void validateSort(Sort sort) {
		boolean hasUnknownField = sort.stream()
			.map(Sort.Order::getProperty)
			.anyMatch(property -> !SORTABLE.contains(property));

		if (hasUnknownField) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}
	}

	/**
	 * 행사 정보를 수정한다. 공공 행사와 회원 제보를 모두 고칠 수 있다.
	 *
	 * 공공 API에서 받은 값이 틀린 경우(장소 오기, 날짜 오류 등)를 바로잡는 것이 주 용도다.
	 * 수동 동기화는 신규 행사만 insert 하고 기존 행을 건드리지 않으므로
	 * 여기서 고친 값이 다음 동기화에 덮이지 않는다.
	 *
	 * TODO 동기화가 나중에 upsert 방식으로 바뀌면 관리자가 고친 행사를 건너뛰도록 해야 한다.
	 *      그러려면 "관리자가 손댔다"는 표시가 필요하다.
	 */
	@Transactional
	public FestivalResponse.DetailResponse update(
		Long festivalId,
		AdminFestivalRequest.Update request
	) {
		if (!request.hasValidPeriod()) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}

		// 삭제된 행사는 고칠 수 없다. 되살리려면 복구가 먼저다.
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		// 회원 제보 수정과 같은 메서드를 쓴다. 진행 상태(status)는 종료일에서 다시 계산된다.
		festival.updateDetails(
			request.instNm(),
			request.title(),
			request.category(),
			request.festivalContent(),
			request.referenceUrl(),
			request.region(),
			request.regionDetail(),
			request.imgUrl(),
			request.beginDe(),
			request.endDe(),
			request.eventTmInfo(),
			request.partcptExpnInfo(),
			request.telnoInfo(),
			request.hostInstNm()
		);

		return FestivalResponse.DetailResponse.from(festival);
	}
}
