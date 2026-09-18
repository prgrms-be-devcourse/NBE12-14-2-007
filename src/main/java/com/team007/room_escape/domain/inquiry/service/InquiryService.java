package com.team007.room_escape.domain.inquiry.service;

import com.team007.room_escape.domain.inquiry.dto.InquiryRequest;
import com.team007.room_escape.domain.inquiry.dto.InquiryResponse;
import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.repository.MemberRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.MemberExceptionCode;
import com.team007.room_escape.global.storage.ImageUrlResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

	private final InquiryRepository inquiryRepository;
	private final MemberRepository memberRepository;
	private final ImageUrlResolver imageUrlResolver;

	/**
	 * 문의를 등록한다. 등급 제한 없이 로그인한 회원이면 누구나 쓸 수 있다.
	 *
	 * 토큰이 유효해도 그 사이 탈퇴했을 수 있으므로 DB에서 살아있는 회원인지 다시 확인한다.
	 */
	@Transactional
	public InquiryResponse.CreateInfo create(UUID memberId, InquiryRequest.Create request) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberExceptionCode.MEMBER_NOT_FOUND));

		// 요청 DTO는 이 줄에서 엔티티로 끝내고, 리포지토리에는 엔티티만 넘긴다.
		Inquiry inquiry = Inquiry.builder()
			.member(member)
			.category(request.category())
			.title(request.title())
			.content(request.content())
			// img는 업로드 API가 돌려준 R2 key다. 공개 URL이 아니다.
			.img(request.img())
			// 등록 직후에는 항상 답변 대기 상태다.
			.status(InquiryStatus.PENDING)
			.build();

		Inquiry saved = inquiryRepository.save(inquiry);

		// img는 R2 key로 저장되므로 응답에서는 공개 URL로 바꿔 내려준다.
		return InquiryResponse.CreateInfo.from(saved, imageUrlResolver.resolve(saved.getImg()));
	}
}
