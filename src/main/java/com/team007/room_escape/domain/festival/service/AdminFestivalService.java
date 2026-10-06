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
import com.team007.room_escape.global.util.RichTextSanitizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFestivalService {

	/** 정렬 허용 필드. 엔티티에 없는 필드가 오면 500이 나서 먼저 거른다. */
	private static final Set<String> SORTABLE =
		Set.of("beginDe", "endDe", "title", "createdAt");

	private final FestivalRepository festivalRepository;
	private final MemberTrustGradeService memberTrustGradeService;
	private final RichTextSanitizer richTextSanitizer;

	/** 관리자 행사 검색. 복구 대상을 찾도록 삭제된 행사도 볼 수 있다. */
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

	/** 행사 삭제. 삭제 시각만 남겨 복구할 수 있고, 달린 후기는 그대로 남는다. */
	@Transactional
	public void delete(Long festivalId) {
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		festival.delete();
		festivalRepository.flush();
		memberTrustGradeService.refreshForFestival(festival);
	}

	/** 삭제된 행사 복구. 누가 지웠는지 몰라서 지금은 공공 행사만 복구한다. */
	// TODO 삭제 주체를 기록하게 되면 회원 제보도 "관리자가 지운 것"만 복구를 열 것.
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

	/** 행사 수정. 동기화는 신규만 insert 해서 여기서 고친 값이 덮이지 않는다. */
	// TODO 동기화가 upsert로 바뀌면 관리자가 고친 행사는 건너뛰도록 표시가 필요하다.
	@Transactional
	public FestivalResponse.Detail update(
		Long festivalId,
		AdminFestivalRequest.Update request
	) {
		if (!request.hasValidPeriod()) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}

		// 삭제된 행사는 복구 후에 고칠 수 있다.
		Festival festival = festivalRepository.findByIdAndDeletedAtIsNull(festivalId)
			.orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));

		// 진행 상태(status)는 종료일에서 다시 계산된다.
		festival.updateDetails(
			request.instNm(),
			request.title(),
			request.category(),
			richTextSanitizer.sanitize(request.festivalContent()),
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

		return FestivalResponse.Detail.from(festival, 0, 0, null, 0, false, null);
	}
}
