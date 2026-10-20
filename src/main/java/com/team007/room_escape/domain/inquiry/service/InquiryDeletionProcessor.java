package com.team007.room_escape.domain.inquiry.service;

import com.team007.room_escape.domain.deletion.infra.entity.DeletionHistory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.InquiryExceptionCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 콘텐츠 삭제 트랜잭션 안에서 연결된 신고 한 건만 처리한다. */
@Service
@RequiredArgsConstructor
public class InquiryDeletionProcessor {
	private final InquiryRepository inquiries;

	@Transactional(propagation = Propagation.MANDATORY)
	public void complete(UUID inquiryId, DeletionHistory deletion) {
		var inquiry = inquiries.findForProcessing(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));
		if (inquiry.isDeleted()) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_ALREADY_DELETED);
		}
		if (inquiry.isAnswered()) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_ALREADY_ANSWERED);
		}
		if (inquiry.getCategory() != InquiryCategory.REPORT || inquiry.getStatus() != InquiryStatus.PENDING
			|| inquiry.getTargetType() == null
			|| !inquiry.getTargetType().name().equals(deletion.getTargetType().name())
			|| !deletion.getTargetId().equals(deletion.getTargetType().normalizeId(inquiry.getTargetId()))) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_TARGET_MISMATCH);
		}
		String answer = "신고된 콘텐츠를 삭제했습니다. 사유: " + deletion.getReason().getDescription();
		if (deletion.getReasonDetail() != null) {
			answer += " — " + deletion.getReasonDetail();
		}
		inquiry.answer(answer);
	}
}
