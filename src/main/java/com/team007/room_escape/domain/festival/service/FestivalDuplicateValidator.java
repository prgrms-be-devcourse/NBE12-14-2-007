package com.team007.room_escape.domain.festival.service;

import java.time.LocalDateTime;
import java.util.Objects;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 등록·수정·복구에서 같은 중복 기준과 잠금 키를 사용한다. 호출자는 트랜잭션을 유지한다. */
@Component
@RequiredArgsConstructor
public class FestivalDuplicateValidator {
	private final FestivalRepository repository;

	public String normalizeReferenceUrl(String url) {
		return url.trim().replaceFirst("/+$", "");
	}

	public void validate(Long currentId,
		LocalDateTime beginDe, LocalDateTime endDe, FestivalRegion region, String referenceUrl) {
		var begin = beginDe.toLocalDate();
		var end = endDe.toLocalDate();
		String url = normalizeReferenceUrl(referenceUrl);
		repository.lockSubmissionDuplicateKey(Objects.hash(begin, end, region.name(), url));
		if (repository.existsSubmissionDuplicate(currentId, begin.atStartOfDay(),
			begin.plusDays(1).atStartOfDay(), end.atStartOfDay(), end.plusDays(1).atStartOfDay(),
			region.name(), url)) {
			throw new BusinessException(FestivalExceptionCode.DUPLICATE_FESTIVAL);
		}
	}
}
