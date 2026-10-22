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

/** 후기 7개: 작성, 목록, 어드민 목록, 행사별 목록, 상세, 수정, 삭제 */
class PostApiContractTest extends ApiContractSupport {

	@Test
	void 후기_목록은_비로그인도_조회한다() throws Exception {
		mvc.perform(get("/api/v1/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 행사별_후기_목록은_비로그인도_조회한다() throws Exception {
		mvc.perform(get("/api/v1/festivals/" + festivalId() + "/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 회원은_행사에_후기를_작성한다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content(postBody("후기 제목입니다", "후기 내용")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").isNotEmpty());
	}

	@Test
	void 제목이_한_글자면_후기를_작성할_수_없다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content(postBody("짧", "내용")))
			.andExpect(status().isBadRequest());
	}

	/** 활동 제한 회원은 후기를 쓸 수 없다. 지금은 등급 계층의 최하단으로 표현된다. */
	@Test
	void 활동_제한_회원은_후기를_작성할_수_없다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"),
				tokenOf(MemberRole.ROLE_WARNING))
				.contentType("application/json")
				.content(postBody("제한 회원 후기", "내용")))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_후기를_작성할_수_없다() throws Exception {
		mvc.perform(post("/api/v1/festivals/" + festivalId() + "/posts")
				.contentType("application/json")
				.content(postBody("비로그인 후기", "내용")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 후기_상세는_비로그인도_조회한다() throws Exception {
		UUID postId = createPost(tokenOf(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(get("/api/v1/posts/" + postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(postId.toString()))
			.andExpect(jsonPath("$.data.member").exists());
	}

	@Test
	void 없는_후기를_조회하면_404다() throws Exception {
		mvc.perform(get("/api/v1/posts/" + UUID.randomUUID()))
			.andExpect(status().isNotFound());
	}

	@Test
	void 작성자는_후기를_수정한다() throws Exception {
		String author = tokenOf(MemberRole.ROLE_UNVERIFIED);
		UUID postId = createPost(author);

		mvc.perform(as(patch("/api/v1/posts/" + postId), author)
				.contentType("application/json")
				.content(postBody("수정된 제목", "수정된 내용")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("수정된 제목"));
	}

	@Test
	void 남의_후기는_수정할_수_없다() throws Exception {
		UUID postId = createPost(tokenOf(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(patch("/api/v1/posts/" + postId), tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content(postBody("남의 글 수정", "내용")))
			.andExpect(status().isForbidden());
	}

	@Test
	void 작성자는_후기를_삭제한다() throws Exception {
		String author = tokenOf(MemberRole.ROLE_UNVERIFIED);
		UUID postId = createPost(author);

		mvc.perform(as(delete("/api/v1/posts/" + postId), author))
			.andExpect(status().isOk());
		mvc.perform(get("/api/v1/posts/" + postId))
			.andExpect(status().isNotFound());
	}

	@Test
	void 남의_후기는_삭제할_수_없다() throws Exception {
		UUID postId = createPost(tokenOf(MemberRole.ROLE_UNVERIFIED));

		mvc.perform(as(delete("/api/v1/posts/" + postId), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 관리자는_삭제된_후기까지_조회한다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/posts"), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 일반_회원은_어드민_후기_목록을_조회할_수_없다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/posts"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	private UUID createPost(String bearerToken) throws Exception {
		var result = mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"), bearerToken)
				.contentType("application/json")
				.content(postBody("계약테스트 후기 " + UUID.randomUUID().toString().substring(0, 8), "내용")))
			.andExpect(status().isOk())
			.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("id").asString());
	}

	private String postBody(String title, String content) {
		return """
			{"title":"%s","content":"%s"}
			""".formatted(title, content);
	}
}
