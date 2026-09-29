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
	/** 첨부를 교체할 때 옛 파일을 지우려면 저장소를 직접 다뤄야 한다. */
	private final R2StorageService r2StorageService;

	/**
	 * 문의를 등록한다. 등급 제한 없이 로그인한 회원이면 누구나 쓸 수 있다.
	 *
	 * 토큰이 유효해도 그 사이 탈퇴했을 수 있으므로 DB에서 살아있는 회원인지 다시 확인한다.
	 */
	@Transactional
	public InquiryResponse.Info create(UUID memberId, InquiryRequest.Create request) {
		Member member = memberReader.getActiveMember(memberId);
		targetValidator.validate(request.targetType(), request.targetId());
		r2StorageService.requireOwnedBy(request.img(), null, memberId);

		// 요청 DTO는 이 줄에서 엔티티로 끝내고, 리포지토리에는 엔티티만 넘긴다.
		Inquiry inquiry = Inquiry.builder()
			.member(member)
			.category(request.category())
			.targetType(request.targetType())
			.targetId(emptyToNull(request.targetId()))
			.title(request.title())
			.content(request.content())
			// img는 업로드 API가 돌려준 R2 key다. 공개 URL이 아니다.
			// 빈 문자열이 저장되면 "첨부 없음"이 null과 "" 두 가지로 갈린다.
			.img(emptyToNull(request.img()))
			// 등록 직후에는 항상 답변 대기 상태다.
			.status(InquiryStatus.PENDING)
			.build();

		Inquiry saved = inquiryRepository.save(inquiry);

		// img는 R2 key로 저장되므로 응답에서는 공개 URL로 바꿔 내려준다.
		return InquiryResponse.Info.from(saved, imageUrlResolver.resolve(saved.getImg()));
	}

	/** 내가 쓴 문의 목록. 최신순. 남의 문의는 애초에 조회 대상이 아니다. */
	@Transactional(readOnly = true)
	public List<InquiryResponse.Info> findMine(UUID memberId) {
		return inquiryRepository.findAllByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId).stream()
			.map(inquiry -> InquiryResponse.Info.from(inquiry, imageUrlResolver.resolve(inquiry.getImg())))
			.toList();
	}

	/**
	 * 문의 상세. 본인이 쓴 것만 볼 수 있다.
	 *
	 * 남의 문의를 조회하면 403이 아니라 404로 응답한다.
	 * 403으로 내려주면 "그 id의 문의가 존재한다"는 사실이 새어나가기 때문이다.
	 */
	@Transactional(readOnly = true)
	public InquiryResponse.Info findMineById(UUID inquiryId, UUID memberId) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.filter(found -> found.isWrittenBy(memberId))
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		return InquiryResponse.Info.from(inquiry, imageUrlResolver.resolve(inquiry.getImg()));
	}

	/**
	 * 문의를 수정한다. 보낸 필드만 반영한다.
	 *
	 * 답변이 달린 뒤에는 수정할 수 없다. 질문이 바뀌면 이미 달린 답변이
	 * 엉뚱한 내용에 대한 답이 되어버리기 때문이다.
	 */
	@Transactional
	public InquiryResponse.Info update(UUID inquiryId, UUID memberId, InquiryRequest.Update request) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		// 수정은 관리자에게도 열지 않는다. 남의 문의 내용을 고칠 이유가 없다.
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

		return InquiryResponse.Info.from(inquiry, imageUrlResolver.resolve(inquiry.getImg()));
	}

	/** 문의를 삭제한다. 작성자 본인과 관리자가 삭제할 수 있다. */
	@Transactional
	public void delete(UUID inquiryId, UUID memberId, boolean isAdmin) {
		Inquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)
			.orElseThrow(() -> new BusinessException(InquiryExceptionCode.INQUIRY_NOT_FOUND));

		if (!inquiry.isWrittenBy(memberId) && !isAdmin) {
			throw new BusinessException(InquiryExceptionCode.INQUIRY_DELETE_FORBIDDEN);
		}

		inquiry.delete();
	}

	/** 교체된 옛 첨부는 R2에서 지운다. 안 지우면 쓰지 않는 파일이 계속 쌓인다. */
	private void deleteReplacedImage(String previousKey, String currentKey, UUID memberId) {
		if (previousKey == null || previousKey.equals(currentKey)) {
			return;
		}
		r2StorageService.deleteOwnedBy(previousKey, memberId);
	}
}
