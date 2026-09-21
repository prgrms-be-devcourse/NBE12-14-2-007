package com.team007.room_escape.domain.inquiry.dto;

import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InquiryRequest {

	private InquiryRequest() {
	}

	@Schema(name = "InquiryCreateRequest", description = "문의 등록 요청")
	public record Create(

		@NotNull
		@Schema(description = "문의 종류. QUESTION(일반 문의) 또는 REPORT(신고)", example = "QUESTION")
		InquiryCategory category,

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
	}

	/**
	 * 문의 수정 요청. PATCH라서 보낸 필드만 반영한다.
	 * 답변이 달린 뒤에는 수정할 수 없다(INQUIRY003).
	 */
	@Schema(name = "InquiryUpdateRequest", description = "문의 수정 요청")
	public record Update(

		@Schema(description = "문의 종류. 생략하면 변경하지 않는다", example = "REPORT")
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
