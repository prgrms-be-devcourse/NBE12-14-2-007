package com.team007.room_escape.domain.inquiry.dto;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class InquiryResponse {

	private InquiryResponse() {
	}

	/** 문의 등록·수정 응답. 두 경우의 모양이 같아 하나로 쓴다. */
	@Builder
	public record Info(
		UUID id,
		InquiryCategory category,
		String title,
		String content,
		/** 공개 URL. 첨부가 없으면 null */
		String img,
		InquiryStatus status,
		/** 관리자 답변. 아직 답변 전이면 null */
		String answer,
		LocalDateTime createdAt
	) {

		/**
		 * @param imgUrl 저장 key를 변환한 공개 URL. 첨부가 없으면 null
		 */
		public static Info from(Inquiry inquiry, String imgUrl) {
			return Info.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.title(inquiry.getTitle())
				.content(inquiry.getContent())
				.img(imgUrl)
				.status(inquiry.getStatus())
				.answer(inquiry.getAnswer())
				.createdAt(inquiry.getCreatedAt())
				.build();
		}
	}
}
