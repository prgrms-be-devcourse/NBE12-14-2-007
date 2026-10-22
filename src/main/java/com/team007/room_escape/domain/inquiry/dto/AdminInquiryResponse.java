package com.team007.room_escape.domain.inquiry.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AdminInquiryResponse {

	private AdminInquiryResponse() {
	}

	/** 관리자 문의 목록 항목. 본문과 답변은 상세에서만 내려준다. */
	@Builder
	@Schema(name = "AdminInquiryListItem", description = "관리자 문의 목록 항목")
	public record ListItem(

		@Schema(description = "문의 번호")
		UUID id,

		@Schema(description = "접수 종류", example = "REPORT")
		InquiryCategory category,

		@Schema(description = "신고·제보 대상 종류. 일반 문의면 null", example = "FESTIVAL")
		InquiryTargetType targetType,

		@Schema(description = "신고·제보 대상 ID. 일반 문의면 null", example = "123")
		String targetId,

		@Schema(description = "제목")
		String title,

		@Schema(description = "작성자. 회원 정보가 없으면 null")
		MemberResponse.AdminInfo writer,

		@Schema(description = "답변 상태", example = "PENDING")
		InquiryStatus status,

		@Schema(description = "등록 시각")
		LocalDateTime createdAt,

		@Schema(description = "삭제 시각. 삭제되지 않았으면 null")
		LocalDateTime deletedAt
	) {

		public static ListItem from(Inquiry inquiry, String writerProfileImgUrl) {
			Member writer = inquiry.getMember();

			return ListItem.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.targetType(inquiry.getTargetType())
				.targetId(inquiry.getTargetId())
				.title(inquiry.getTitle())
				.writer(writer == null ? null : MemberResponse.AdminInfo.from(writer, writerProfileImgUrl))
				.status(inquiry.getStatus())
				.createdAt(inquiry.getCreatedAt())
				.deletedAt(inquiry.getDeletedAt())
				.build();
		}
	}

	/** 관리자 문의 상세. 본문·첨부·답변까지 내려준다. */
	@Builder
	@Schema(name = "AdminInquiryDetail", description = "관리자 문의 상세")
	public record Detail(

		@Schema(description = "문의 번호")
		UUID id,

		@Schema(description = "접수 종류", example = "REPORT")
		InquiryCategory category,

		@Schema(description = "신고·제보 대상 종류. 일반 문의면 null", example = "FESTIVAL")
		InquiryTargetType targetType,

		@Schema(description = "신고·제보 대상 ID. 일반 문의면 null", example = "123")
		String targetId,

		@Schema(description = "댓글 신고일 때 그 댓글이 달린 글 ID. 그 외에는 null")
		UUID targetPostId,

		@Schema(description = "제목")
		String title,

		@Schema(description = "본문")
		String content,

		@Schema(description = "첨부 이미지 공개 URL. 첨부가 없으면 null")
		String img,

		@Schema(description = "작성자. 회원 정보가 없으면 null")
		MemberResponse.AdminInfo writer,

		@Schema(description = "답변 상태", example = "PENDING")
		InquiryStatus status,

		@Schema(description = "관리자 답변. 아직 답변 전이면 null")
		String answer,

		@Schema(description = "등록 시각")
		LocalDateTime createdAt,

		@Schema(description = "최종 수정 시각")
		LocalDateTime updatedAt,

		@Schema(description = "삭제 시각. 삭제되지 않았으면 null")
		LocalDateTime deletedAt
	) {

		public static Detail from(
			Inquiry inquiry,
			String imgUrl,
			String writerProfileImgUrl,
			UUID targetPostId
		) {
			Member writer = inquiry.getMember();

			return Detail.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.targetType(inquiry.getTargetType())
				.targetId(inquiry.getTargetId())
				.targetPostId(targetPostId)
				.title(inquiry.getTitle())
				.content(inquiry.getContent())
				.img(imgUrl)
				.writer(writer == null ? null : MemberResponse.AdminInfo.from(writer, writerProfileImgUrl))
				.status(inquiry.getStatus())
				.answer(inquiry.getAnswer())
				.createdAt(inquiry.getCreatedAt())
				.updatedAt(inquiry.getUpdatedAt())
				.deletedAt(inquiry.getDeletedAt())
				.build();
		}
	}
}
