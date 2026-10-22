package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 후기 댓글 5개: 어드민 목록, 작성, 목록, 수정, 삭제 */
class CommentApiContractTest extends ApiContractSupport {

	@Test
	void 댓글_목록은_비로그인도_조회한다() throws Exception {
		UUID postId = createPost();

		mvc.perform(get("/api/v1/posts/" + postId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 회원은_댓글을_작성한다() throws Exception {
		UUID postId = createPost();

		mvc.perform(as(post("/api/v1/posts/" + postId + "/comments"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"content":"댓글 내용"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").isNotEmpty())
			.andExpect(jsonPath("$.data.content").value("댓글 내용"));
	}

	@Test
	void 빈_댓글은_작성할_수_없다() throws Exception {
		UUID postId = createPost();

		mvc.perform(as(post("/api/v1/posts/" + postId + "/comments"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"content":"   "}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 활동_제한_회원은_댓글을_작성할_수_없다() throws Exception {
		UUID postId = createPost();

		mvc.perform(as(post("/api/v1/posts/" + postId + "/comments"),
				tokenOf(MemberRole.ROLE_WARNING))
				.contentType("application/json")
				.content("""
					{"content":"제한 회원 댓글"}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_댓글을_작성할_수_없다() throws Exception {
		UUID postId = createPost();

		mvc.perform(post("/api/v1/posts/" + postId + "/comments")
				.contentType("application/json")
				.content("""
					{"content":"비로그인 댓글"}
					"""))
			.andExpect(status().isUnauthorized());
	}

	/** 대댓글은 parentId 로 달리고, 응답에서 부모의 replies 에 담긴다. */
	@Test
	void 대댓글은_부모의_replies에_담긴다() throws Exception {
		UUID postId = createPost();
		String author = tokenOf(MemberRole.ROLE_UNVERIFIED);
		long parentId = createComment(postId, author, null);
		createComment(postId, author, parentId);

		mvc.perform(get("/api/v1/posts/" + postId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].id").value((int) parentId))
			.andExpect(jsonPath("$.data.content[0].replies").isArray())
			.andExpect(jsonPath("$.data.content[0].replies[0].parentId").value((int) parentId));
	}

	@Test
	void 작성자는_댓글을_수정한다() throws Exception {
		UUID postId = createPost();
		String author = tokenOf(MemberRole.ROLE_UNVERIFIED);
		long commentId = createComment(postId, author, null);

		mvc.perform(as(patch("/api/v1/comments/" + commentId), author)
				.contentType("application/json")
				.content("""
					{"content":"수정된 댓글"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").value("수정된 댓글"));
	}

	@Test
	void 남의_댓글은_수정할_수_없다() throws Exception {
		UUID postId = createPost();
		long commentId = createComment(postId, tokenOf(MemberRole.ROLE_UNVERIFIED), null);

		mvc.perform(as(patch("/api/v1/comments/" + commentId), tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"content":"남의 댓글 수정"}
					"""))
			.andExpect(status().isForbidden());
	}

	/** 삭제해도 행은 남기고 "삭제된 댓글입니다"로 내려간다. 대댓글 흐름이 끊기지 않게 하는 정책이다. */
	@Test
	void 삭제된_댓글은_목록에서_안내_문구로_내려간다() throws Exception {
		UUID postId = createPost();
		String author = tokenOf(MemberRole.ROLE_UNVERIFIED);
		long commentId = createComment(postId, author, null);

		mvc.perform(as(delete("/api/v1/comments/" + commentId), author))
			.andExpect(status().isOk());

		mvc.perform(get("/api/v1/posts/" + postId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].id").value((int) commentId))
			.andExpect(jsonPath("$.data.content[0].deleted").value(true));
	}

	@Test
	void 남의_댓글은_삭제할_수_없다() throws Exception {
		UUID postId = createPost();
		long commentId = createComment(postId, tokenOf(MemberRole.ROLE_UNVERIFIED), null);

		mvc.perform(as(delete("/api/v1/comments/" + commentId), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 관리자는_삭제된_댓글의_원문을_조회한다() throws Exception {
		UUID postId = createPost();

		mvc.perform(as(get("/api/v1/admin/posts/" + postId + "/comments"),
				tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 일반_회원은_어드민_댓글_목록을_조회할_수_없다() throws Exception {
		UUID postId = createPost();

		mvc.perform(as(get("/api/v1/admin/posts/" + postId + "/comments"),
				tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	private UUID createPost() throws Exception {
		var result = mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"title":"댓글테스트 후기 %s","content":"내용"}
					""".formatted(UUID.randomUUID().toString().substring(0, 8))))
			.andExpect(status().isOk())
			.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("id").asString());
	}

	private long createComment(UUID postId, String bearerToken, Long parentId) throws Exception {
		String body = parentId == null
			? "{\"content\":\"댓글 내용\"}"
			: "{\"content\":\"대댓글 내용\",\"parentId\":%d}".formatted(parentId);
		var result = mvc.perform(as(post("/api/v1/posts/" + postId + "/comments"), bearerToken)
				.contentType("application/json").content(body))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("id").asLong();
	}
}
