package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import org.junit.jupiter.api.Test;

/** GET /admin/members, GET /admin/members/{id}, PATCH /admin/members/{id}/role */
class AdminMemberApiContractTest extends ApiContractSupport {

	@Test
	void 관리자는_회원_목록을_조회한다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/members"), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 일반_회원은_회원_목록을_조회할_수_없다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/members"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_회원_목록을_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/admin/members"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 관리자는_회원_단건을_조회한다() throws Exception {
		Member target = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(get("/api/v1/admin/members/" + target.getId()), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(target.getId().toString()))
			.andExpect(jsonPath("$.data.email").value(target.getEmail()));
	}

	@Test
	void 일반_회원은_회원_단건을_조회할_수_없다() throws Exception {
		Member target = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(get("/api/v1/admin/members/" + target.getId()),
				tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 관리자는_회원_등급을_변경한다() throws Exception {
		Member target = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/admin/members/" + target.getId() + "/role"),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content("{\"role\":\"ROLE_TRUSTED\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role").value("ROLE_TRUSTED"));
	}

	@Test
	void 일반_회원은_등급을_변경할_수_없다() throws Exception {
		Member target = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/admin/members/" + target.getId() + "/role"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("{\"role\":\"ROLE_TRUSTED\"}"))
			.andExpect(status().isForbidden());
	}

	/** 관리자 권한은 화면에서 주지 않는다. DB에서 직접 바꾸는 정책이다. */
	@Test
	void 관리자_권한은_API로_부여할_수_없다() throws Exception {
		Member target = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/admin/members/" + target.getId() + "/role"),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content("{\"role\":\"ROLE_ADMIN\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("MEMBER005"));
	}
}
