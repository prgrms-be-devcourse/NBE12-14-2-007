package com.team007.room_escape.domain.inquiry.dto;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

public class InquiryResponse {

	private InquiryResponse() {
	}

	/** 내 문의 목록 항목. 본문과 답변은 상세에서만 내려준다. */
	@Builder
	public record ListItem(
		UUID id,
		InquiryCategory category,
		InquiryTargetType targetType,
		String targetId,
		String title,
		InquiryStatus status,
		LocalDateTime createdAt
	) {

		public static ListItem from(Inquiry inquiry) {
			return ListItem.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.targetType(inquiry.getTargetType())
				.targetId(inquiry.getTargetId())
				.title(inquiry.getTitle())
				.status(inquiry.getStatus())
				.createdAt(inquiry.getCreatedAt())
				.build();
		}
	}

	/** 문의 상세. 등록·수정 응답도 같은 모양이다. */
	@Builder
	public record Detail(
		UUID id,
		InquiryCategory category,
		InquiryTargetType targetType,
		String targetId,
		String title,
		String content,
		/** 공개 URL. 첨부가 없으면 null */
		String img,
		InquiryStatus status,
		/** 관리자 답변. 아직 답변 전이면 null */
		String answer,
		LocalDateTime createdAt
	) {

		public static Detail from(Inquiry inquiry, String imgUrl) {
			return Detail.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.targetType(inquiry.getTargetType())
				.targetId(inquiry.getTargetId())
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
