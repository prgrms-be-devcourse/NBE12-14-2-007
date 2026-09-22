package com.team007.room_escape.domain.inquiry.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.inquiry.dto.AdminInquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.AdminInquiryResponse;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

	/**
	 * 정렬에 허용하는 필드.
	 * Pageable 의 sort 는 클라이언트가 아무 이름이나 넣을 수 있어서,
	 * 엔티티에 없는 필드가 들어오면 500이 난다. 그래서 먼저 걸러낸다.
	 */
	private static final Set<String> SORTABLE = Set.of("createdAt", "status", "category");

	private final InquiryRepository inquiryRepository;
	private final ImageUrlResolver imageUrlResolver;

	/** 관리자 문의 검색. 조건을 비우면 전체를 조회한다. */
	@Transactional(readOnly = true)
	public Page<AdminInquiryResponse.ListItem> search(
		AdminInquiryRequest.Search request,
		Pageable pageable
	) {
		validateSort(pageable.getSort());

		return inquiryRepository.search(
			request.titleOrEmpty(),
			request.status(),
			request.category(),
			request.includeDeletedOrFalse(),
			pageable
		).map(inquiry -> AdminInquiryResponse.ListItem.from(
			inquiry,
			inquiry.getMember() == null
				? null
				: imageUrlResolver.resolve(inquiry.getMember().getProfileImg())
		));
	}

	private void validateSort(Sort sort) {
		boolean hasUnknownField = sort.stream()
			.map(Sort.Order::getProperty)
			.anyMatch(property -> !SORTABLE.contains(property));

		if (hasUnknownField) {
			throw new BusinessException(CommonExceptionCode.INVALID_INPUT);
		}
	}
}
