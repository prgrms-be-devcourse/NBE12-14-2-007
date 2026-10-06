package com.team007.room_escape.domain.inquiry.service;

import static com.team007.room_escape.global.util.StringUtil.emptyToNull;

import com.team007.room_escape.domain.inquiry.dto.InquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.InquiryResponse;
import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.service.MemberReader;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.storage.R2StorageService;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

	private final InquiryRepository inquiryRepository;
	private final MemberReader memberReader;
	private final InquiryTargetValidator targetValidator;
	private final ImageUrlResolver imageUrlResolver;
	private final R2StorageService r2StorageService;

	/** 문의 등록. 로그인한 회원이면 등급과 상관없이 쓸 수 있다. */
	@Transactional
	public InquiryResponse.Detail create(UUID memberId, InquiryRequest.Create request) {
		Member member = memberReader.getActiveMember(memberId);
		targetValidator.validate(request.targetType(), request.targetId());
		r2StorageService.requireOwnedBy(request.img(), null, memberId);

		Inquiry inquiry = Inquiry.builder()
			.member(member)
			.category(request.category())
			.targetType(request.targetType())
			.targetId(emptyToNull(request.targetId()))
			.title(request.title())
			.content(request.content())
			// 첨부 없음을 null 하나로 맞춘다.
			.img(emptyToNull(request.img()))
			.status(InquiryStatus.PENDING)
			.build();

		Inquiry saved = inquiryRepository.save(inquiry);

		return InquiryResponse.Detail.from(saved, imageUrlResolver.resolve(saved.getImg()));
	}

	/** 내 문의 목록. 최신순. */
	@Transactional(readOnly = true)
	public List<InquiryResponse.ListItem> findMine(UUID memberId) {
		return inquiryRepository.findAllByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId).stream()
			.map(InquiryResponse.ListItem::from)
			.toList();
	}

	/** 내 문의 상세. 남의 문의는 존재를 숨기려고 404로 응답한다. */
	@Transactional(readOnly = true)
	public InquiryResponse.Detail findMineById(UUID inquiryId, UUID memberId) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.filter(found -> found.isWrittenBy(memberId))
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		return InquiryResponse.Detail.from(inquiry, imageUrlResolver.resolve(inquiry.getImg()));
	}

	/** 문의 수정. 답변이 달린 뒤에는 수정할 수 없다. */
	@Transactional
	public InquiryResponse.Detail update(UUID inquiryId, UUID memberId, InquiryRequest.Update request) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		// 수정은 관리자도 불가.
		if (!inquiry.isWrittenBy(memberId)) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_UPDATE_FORBIDDEN);
		}
		if (inquiry.isAnswered()) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_ALREADY_ANSWERED);
		}

		r2StorageService.requireOwnedBy(request.img(), inquiry.getImg(), memberId);

		String previousImg = inquiry.getImg();
		inquiry.update(request.category(), request.title(), request.content(), request.img());

		deleteReplacedImage(previousImg, inquiry.getImg(), memberId);

		return InquiryResponse.Detail.from(inquiry, imageUrlResolver.resolve(inquiry.getImg()));
	}

	/** 문의 삭제. 작성자 본인과 관리자가 삭제할 수 있다. */
	@Transactional
	public void delete(UUID inquiryId, UUID memberId, boolean isAdmin) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		if (!inquiry.isWrittenBy(memberId) && !isAdmin) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_DELETE_FORBIDDEN);
		}

		inquiry.delete();
	}

	/** 교체된 옛 첨부를 R2에서 지운다. */
	private void deleteReplacedImage(String previousKey, String currentKey, UUID memberId) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.deleteOwnedBy(previousKey, memberId);
	}
}
