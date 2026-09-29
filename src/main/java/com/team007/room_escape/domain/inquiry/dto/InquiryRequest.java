package com.team007.room_escape.domain.inquiry.dto;

import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryTargetType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InquiryRequest {

	private InquiryRequest() {
	}

	@Schema(name = "InquiryCreateRequest", description = "문의 등록 요청")
	public record Create(

		@NotNull
		@Schema(description = "접수 종류. QUESTION(일반 문의), REPORT(신고), TIP(제보)", example = "QUESTION")
		InquiryCategory category,

		@Schema(description = "신고·제보 대상 종류. FESTIVAL, POST, COMMENT, MEMBER. 일반 문의는 생략", example = "FESTIVAL")
		InquiryTargetType targetType,

		@Size(max = 255)
		@Schema(description = "신고·제보 대상 ID. 일반 문의는 생략", example = "123")
		String targetId,

		@NotBlank
		@Size(max = 255)
		@Schema(description = "제목", example = "행사 신청이 취소되지 않습니다")
		String title,

		@NotBlank
		@Schema(description = "내용", example = "신청 취소 버튼을 눌러도 목록에 그대로 남아 있습니다.")
		String content,

		@Size(max = 2048)
		@Schema(
			description = "첨부 이미지 key. 업로드 API(/api/v1/images)가 돌려준 key를 넣는다. "
				+ "URL이 아니라 key다. 선택 항목",
			example = "inquiries/0befc150-badb-4674-a99d-ded96f03814a.png"
		)
		String img
	) {
		@JsonIgnore
		@AssertTrue(message = "신고·제보 대상 종류와 대상 ID는 함께 입력해야 합니다.")
		public boolean isTargetValid() {
			boolean targetIdMissing = targetId == null || targetId.isBlank();
			boolean noTarget = targetType == null && targetIdMissing;
			boolean completeTarget = targetType != null && !targetIdMissing;

			if (category == InquiryCategory.QUESTION) {
				return noTarget;
			}
			return noTarget || completeTarget;
		}
	}

	/**
	 * 문의 수정 요청. PATCH라서 보낸 필드만 반영한다.
	 * 답변이 달린 뒤에는 수정할 수 없다(INQUIRY003).
	 */
	@Schema(name = "InquiryUpdateRequest", description = "문의 수정 요청")
	public record Update(

		@Schema(description = "접수 종류. 생략하면 변경하지 않는다", example = "REPORT")
		InquiryCategory category,

		@Size(max = 255)
		@Schema(description = "제목. 생략하면 변경하지 않는다", example = "제목을 수정합니다")
		String title,

		@Schema(description = "내용. 생략하면 변경하지 않는다", example = "내용을 수정합니다")
		String content,

		@Size(max = 2048)
		@Schema(
			description = "첨부 이미지 key. 생략하면 변경하지 않고, 빈 문자열이면 첨부를 지운다",
			example = "inquiries/0befc150-badb-4674-a99d-ded96f03814a.png"
		)
		String img
	) {
	}
}
