package com.team007.room_escape.domain.inquiry.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team007.room_escape.domain.comment.infra.repository.CommentRepository;
import com.team007.room_escape.domain.community.infra.repository.CommunityCommentRepository;
import com.team007.room_escape.domain.inquiry.dto.AdminInquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.AdminInquiryResponse;
import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.CommonExceptionCode;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

	private static final Set<String> SORTABLE = Set.of("createdAt", "status", "category");

	private final InquiryRepository inquiryRepository;
	private final CommentRepository commentRepository;
	private final CommunityCommentRepository communityCommentRepository;
	private final ImageUrlResolver imageUrlResolver;

	/** 관리자 문의 검색. 조건을 비우면 전체를 조회한다. */
	@Transactional(readOnly = true)
	public Page<AdminInquiryResponse.ListItem> search(AdminInquiryRequest.Search request, Pageable pageable) {
		validateSort(pageable.getSort());

		return inquiryRepository.search(
			request.titleOrEmpty(),
			request.status(),
			request.category(),
			request.includeDeletedOrFalse(),
			pageable
		).map(inquiry -> AdminInquiryResponse.ListItem.from(
			inquiry,
			inquiry.getMember() == null ? null : imageUrlResolver.resolve(inquiry.getMember().getProfileImg())));
	}

	/**
	 * 관리자 문의 상세. 삭제된 문의도 열어볼 수 있다.
	 */
	@Transactional(readOnly = true)
	public AdminInquiryResponse.Detail findById(UUID inquiryId) {
		return toDetail(findOrThrow(inquiryId));
	}

	/**
	 * 관리자 답변을 등록한다. 이미 답변이 있으면 덮어쓴다.
	 * 삭제된 문의에는 답변하지 않는다. 작성자가 이미 지운 글이라
	 * 답변을 남겨도 작성자에게 보이지 않기 때문이다.
	 */
	@Transactional
	public AdminInquiryResponse.Detail answer(UUID inquiryId, AdminInquiryRequest.Answer request) {
		Inquiry inquiry = findOrThrow(inquiryId);

		if (inquiry.isDeleted()) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_ALREADY_DELETED);
		}

		inquiry.answer(request.trimmed());

		return toDetail(inquiry);
	}

	private Inquiry findOrThrow(UUID inquiryId) {
		return inquiryRepository.findDetailById(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));
	}

	/** 저장된 key를 공개 URL로 바꿔 채운다. 회원이 없으면 프로필 URL도 없다. */
	private AdminInquiryResponse.Detail toDetail(Inquiry inquiry) {
		return AdminInquiryResponse.Detail.from(
			inquiry,
			imageUrlResolver.resolve(inquiry.getImg()),
			inquiry.getMember() == null
				? null
				: imageUrlResolver.resolve(inquiry.getMember().getProfileImg()),
			findCommentPostId(inquiry)
		);
	}

	/**
	 * 댓글에는 따로 볼 화면이 없어서, 관리자가 신고된 댓글을 확인하려면 댓글이 달린 글로 가야 한다.
	 * 후기 댓글이면 후기 ID, 커뮤니티 댓글이면 커뮤니티 글 ID를 돌려준다.
	 * 목록은 한 번에 여러 건이라 상세에서만 조회한다.
	 */
	private UUID findCommentPostId(Inquiry inquiry) {
		InquiryTargetType type = inquiry.getTargetType();
		if (type != InquiryTargetType.COMMENT && type != InquiryTargetType.COMMUNITY_COMMENT) {
			return null;
		}
		try {
			Long commentId = Long.valueOf(inquiry.getTargetId());
			return type == InquiryTargetType.COMMENT
				? commentRepository.findById(commentId)
					.map(comment -> comment.getPost().getId())
					.orElse(null)
				: communityCommentRepository.findById(commentId)
					.map(comment -> comment.getPost().getId())
					.orElse(null);
		} catch (NumberFormatException e) {
			return null;
		}
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
