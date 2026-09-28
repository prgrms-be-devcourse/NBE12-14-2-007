package com.team007.room_escape.domain.inquiry.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;
import com.team007.room_escape.domain.member.dto.MemberResponse;
import com.team007.room_escape.domain.member.infra.entity.Member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class AdminInquiryResponse {

	private AdminInquiryResponse() {
	}

	/**
	 * 관리자 문의 목록의 한 줄. 목록에서는 본문과 답변을 내려주지 않는다.
	 * 관리자라도 필요한 만큼만 보는 편이 안전하고, 목록 응답도 가벼워진다.
	 */
	@Builder
	@Schema(name = "AdminInquiryListItem", description = "관리자 문의 목록 항목")
	public record ListItem(

		@Schema(description = "문의 번호")
		UUID id,

		@Schema(description = "문의 종류", example = "REPORT")
		InquiryCategory category,

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

		/**
		 * @param writerProfileImgUrl 작성자 프로필 이미지의 공개 URL. 없으면 null
		 */
		public static ListItem from(Inquiry inquiry, String writerProfileImgUrl) {
			Member writer = inquiry.getMember();

			return ListItem.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
				.title(inquiry.getTitle())
				.writer(writer == null ? null : MemberResponse.AdminInfo.from(writer, writerProfileImgUrl))
				.status(inquiry.getStatus())
				.createdAt(inquiry.getCreatedAt())
				.deletedAt(inquiry.getDeletedAt())
				.build();
		}
	}

	/**
	 * 관리자 문의 상세. 목록과 달리 본문·첨부·답변까지 내려준다.
	 * 답변을 쓰려면 관리자가 무엇에 답하는지 봐야 하므로 상세에서만 본문을 연다.
	 */
	@Builder
	@Schema(name = "AdminInquiryDetail", description = "관리자 문의 상세")
	public record Detail(

		@Schema(description = "문의 번호")
		UUID id,

		@Schema(description = "문의 종류", example = "REPORT")
		InquiryCategory category,

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

		/**
		 * @param imgUrl              첨부 이미지의 공개 URL. 없으면 null
		 * @param writerProfileImgUrl 작성자 프로필 이미지의 공개 URL. 없으면 null
		 */
		public static Detail from(Inquiry inquiry, String imgUrl, String writerProfileImgUrl) {
			Member writer = inquiry.getMember();

			return Detail.builder()
				.id(inquiry.getId())
				.category(inquiry.getCategory())
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
