package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** GET /admin/inquiries, GET /admin/inquiries/{id}, PATCH /admin/inquiries/{id}/answer */
class AdminInquiryApiContractTest extends ApiContractSupport {

	@Autowired
	private InquiryRepository inquiryRepository;

	@Test
	void 관리자는_문의_목록을_조회한다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/inquiries"), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 일반_회원은_문의_목록을_조회할_수_없다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/inquiries"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_문의_목록을_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/admin/inquiries"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 관리자는_문의_상세를_조회한다() throws Exception {
		Inquiry inquiry = saveInquiry();

		mvc.perform(as(get("/api/v1/admin/inquiries/" + inquiry.getId()),
				tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(inquiry.getId().toString()))
			.andExpect(jsonPath("$.data.title").value(inquiry.getTitle()));
	}

	@Test
	void 없는_문의를_조회하면_404다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/inquiries/" + UUID.randomUUID()),
				tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isNotFound());
	}

	@Test
	void 일반_회원은_문의_상세를_조회할_수_없다() throws Exception {
		Inquiry inquiry = saveInquiry();

		mvc.perform(as(get("/api/v1/admin/inquiries/" + inquiry.getId()),
				tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 관리자는_문의에_답변한다() throws Exception {
		Inquiry inquiry = saveInquiry();

		mvc.perform(as(patch("/api/v1/admin/inquiries/" + inquiry.getId() + "/answer"),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content("{\"answer\":\"확인 후 처리했습니다.\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.answer").value("확인 후 처리했습니다."));
	}

	@Test
	void 빈_답변은_거부한다() throws Exception {
		Inquiry inquiry = saveInquiry();

		mvc.perform(as(patch("/api/v1/admin/inquiries/" + inquiry.getId() + "/answer"),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content("{\"answer\":\"   \"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 일반_회원은_문의에_답변할_수_없다() throws Exception {
		Inquiry inquiry = saveInquiry();

		mvc.perform(as(patch("/api/v1/admin/inquiries/" + inquiry.getId() + "/answer"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("{\"answer\":\"처리했습니다.\"}"))
			.andExpect(status().isForbidden());
	}

	private Inquiry saveInquiry() {
		return inquiryRepository.saveAndFlush(Inquiry.builder()
			.member(saveMember(MemberRole.ROLE_UNVERIFIED))
			.category(InquiryCategory.QUESTION)
			.title("계약테스트 문의 " + UUID.randomUUID().toString().substring(0, 8))
			.content("문의 내용")
			.build());
	}
}
