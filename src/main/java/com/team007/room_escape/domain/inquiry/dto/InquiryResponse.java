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

	/** 문의 등록 결과. */
	@Builder
	public record CreateInfo(
		UUID id,
		InquiryCategory category,
		String title,
		String content,
		/** 공개 URL. 첨부가 없으면 null */
		String img,
		InquiryStatus status,
		LocalDateTime createdAt
	) {

		/**
		 * @param imgUrl 저장 key를 변환한 공개 URL. 첨부가 없으면 null
		 */
		public static CreateInfo from(Inquiry inquiry, String imgUrl) {
			return CreateInfo.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.title(inquiry.getTitle())
				.content(inquiry.getContent())
				.img(imgUrl)
				.status(inquiry.getStatus())
				.createdAt(inquiry.getCreatedAt())
				.build();
		}
	}
}
