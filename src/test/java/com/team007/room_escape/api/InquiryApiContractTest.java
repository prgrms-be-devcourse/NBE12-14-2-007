package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 문의 5개: 작성, 내 목록, 내 상세, 수정, 삭제 */
class InquiryApiContractTest extends ApiContractSupport {

	@Test
	void 회원은_문의를_등록한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(post("/api/v1/inquiries"), bearer(me))
				.contentType("application/json")
				.content(createBody("문의 제목", "문의 내용")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").isNotEmpty())
			.andExpect(jsonPath("$.data.title").value("문의 제목"));
	}

	@Test
	void 제목이_없으면_문의를_등록할_수_없다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(post("/api/v1/inquiries"), bearer(me))
				.contentType("application/json")
				.content(createBody("   ", "문의 내용")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 없는_대상_종류를_주면_거부한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(post("/api/v1/inquiries"), bearer(me))
				.contentType("application/json")
				.content("""
					{"category":"QUESTION","targetType":"NOPE","title":"제목","content":"내용"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 미인증_사용자는_문의를_등록할_수_없다() throws Exception {
		mvc.perform(post("/api/v1/inquiries")
				.contentType("application/json")
				.content(createBody("비로그인 문의", "내용")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 활동_제한_회원도_문의는_등록한다() throws Exception {
		Member restricted = saveMember(MemberRole.ROLE_WARNING);

		mvc.perform(as(post("/api/v1/inquiries"), bearer(restricted))
				.contentType("application/json")
				.content(createBody("제한 회원 문의", "내용")))
			.andExpect(status().isCreated());
	}

	@Test
	void 내가_쓴_문의_목록을_조회한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		createInquiry(me);

		mvc.perform(as(get("/api/v1/inquiries/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0].id").isNotEmpty());
	}

	@Test
	void 미인증_사용자는_내_문의_목록을_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/inquiries/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 내_문의_상세를_조회한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		UUID inquiryId = createInquiry(me);

		mvc.perform(as(get("/api/v1/inquiries/" + inquiryId), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(inquiryId.toString()));
	}

	/** 남의 문의는 존재 여부도 알려주지 않는다. */
	@Test
	void 남의_문의는_404로_가린다() throws Exception {
		UUID inquiryId = createInquiry(saveMember(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(get("/api/v1/inquiries/" + inquiryId),
				bearer(saveMember(MemberRole.ROLE_UNVERIFIED))))
			.andExpect(status().isNotFound());
	}

	@Test
	void 없는_문의를_조회하면_404다() throws Exception {
		mvc.perform(as(get("/api/v1/inquiries/" + UUID.randomUUID()),
				bearer(saveMember(MemberRole.ROLE_UNVERIFIED))))
			.andExpect(status().isNotFound());
	}

	@Test
	void 작성자는_문의를_수정한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		UUID inquiryId = createInquiry(me);

		mvc.perform(as(patch("/api/v1/inquiries/" + inquiryId), bearer(me))
				.contentType("application/json")
				.content("""
					{"title":"수정된 제목"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("수정된 제목"));
	}

	@Test
	void 남의_문의는_수정할_수_없다() throws Exception {
		UUID inquiryId = createInquiry(saveMember(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(patch("/api/v1/inquiries/" + inquiryId),
				bearer(saveMember(MemberRole.ROLE_UNVERIFIED)))
				.contentType("application/json")
				.content("""
					{"title":"남의 문의 수정"}
					"""))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 작성자는_문의를_삭제한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		UUID inquiryId = createInquiry(me);

		mvc.perform(as(delete("/api/v1/inquiries/" + inquiryId), bearer(me)))
			.andExpect(status().isOk());
		mvc.perform(as(get("/api/v1/inquiries/" + inquiryId), bearer(me)))
			.andExpect(status().isNotFound());
	}

	@Test
	void 관리자는_남의_문의도_삭제한다() throws Exception {
		UUID inquiryId = createInquiry(saveMember(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(delete("/api/v1/inquiries/" + inquiryId), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk());
	}

	@Test
	void 남의_문의는_삭제할_수_없다() throws Exception {
		UUID inquiryId = createInquiry(saveMember(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(delete("/api/v1/inquiries/" + inquiryId),
				bearer(saveMember(MemberRole.ROLE_UNVERIFIED))))
			.andExpect(status().is4xxClientError());
	}

	private UUID createInquiry(Member author) throws Exception {
		var result = mvc.perform(as(post("/api/v1/inquiries"), bearer(author))
				.contentType("application/json")
				.content(createBody("계약테스트 문의 " + shortToken(), "내용")))
			.andExpect(status().isCreated())
			.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("id").asString());
	}

	private String createBody(String title, String content) {
		return """
			{"category":"QUESTION","title":"%s","content":"%s"}
			""".formatted(title, content);
	}

	private String shortToken() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
