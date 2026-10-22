package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 공개 행사 3개, 좋아요 4개, 정확도 평가 2개, 제보 5개, 날씨 1개, 이미지 1개.
 *
 * 공개 조회 범위와 로그인 요구 여부가 핵심이다. 외부 API 를 타는 동기화와 날씨는
 * 성공 응답까지 보장할 수 없어, 인가와 파라미터 검증만 고정한다.
 */
class FestivalApiContractTest extends ApiContractSupport {

	// ----- 공개 행사 조회 -----

	@Test
	void 행사_목록은_비로그인도_조회한다() throws Exception {
		mvc.perform(get("/api/v1/festivals"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	/**
	 * 공개 목록에는 어드민과 달리 정렬 화이트리스트가 없다.
	 * 엔티티에 있는 필드는 그대로 통과하고, 없는 필드만 400으로 걸린다.
	 */
	@Test
	void 엔티티에_없는_정렬_필드는_거부한다() throws Exception {
		mvc.perform(get("/api/v1/festivals?sort=nopeField,desc"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 행사_상세는_비로그인도_조회한다() throws Exception {
		mvc.perform(get("/api/v1/festivals/" + festivalId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.festivalId").value(festivalId()));
	}

	@Test
	void 없는_행사를_조회하면_404다() throws Exception {
		mvc.perform(get("/api/v1/festivals/99999999"))
			.andExpect(status().isNotFound());
	}

	@Test
	void 관리자만_공공_행사_동기화를_실행한다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/sync"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/festivals/sync"))
			.andExpect(status().isUnauthorized());
	}

	// ----- 좋아요 -----

	@Test
	void 행사_좋아요를_누르고_취소한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/likes/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likeCount").value(1));

		mvc.perform(as(delete("/api/v1/festivals/" + festivalId() + "/likes/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likeCount").value(0));
	}

	@Test
	void 미인증_사용자는_행사_좋아요를_누를_수_없다() throws Exception {
		mvc.perform(post("/api/v1/festivals/" + festivalId() + "/likes/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 활동_제한_회원은_행사_좋아요를_누를_수_없다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/likes/me"),
				tokenOf(MemberRole.ROLE_WARNING)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 후기_좋아요를_누르고_취소한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		UUID postId = createPost();

		mvc.perform(as(post("/api/v1/posts/" + postId + "/likes/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likeCount").value(1));

		mvc.perform(as(delete("/api/v1/posts/" + postId + "/likes/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.likeCount").value(0));
	}

	@Test
	void 미인증_사용자는_후기_좋아요를_누를_수_없다() throws Exception {
		mvc.perform(post("/api/v1/posts/" + createPost() + "/likes/me"))
			.andExpect(status().isUnauthorized());
	}

	// ----- 정확도 평가 -----

	/** 공공 API 가 가져온 행사는 회원 제보가 아니라 평가 대상이 아니다. */
	@Test
	void 공공_행사는_정확도를_평가할_수_없다() throws Exception {
		mvc.perform(as(put("/api/v1/festivals/" + festivalId() + "/accuracy-votes/me"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"voteType":"ACCURATE"}
					"""))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 평가_종류가_없으면_거부한다() throws Exception {
		mvc.perform(as(put("/api/v1/festivals/" + festivalId() + "/accuracy-votes/me"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 미인증_사용자는_정확도를_평가할_수_없다() throws Exception {
		mvc.perform(put("/api/v1/festivals/" + festivalId() + "/accuracy-votes/me")
				.contentType("application/json")
				.content("""
					{"voteType":"ACCURATE"}
					"""))
			.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/v1/festivals/" + festivalId() + "/accuracy-votes/me"))
			.andExpect(status().isUnauthorized());
	}

	// ----- 행사 제보 -----

	@Test
	void 회원은_행사를_제보한다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/submissions"), tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content(submissionBody("제보 행사 " + shortToken())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	void 참고_링크가_없으면_제보할_수_없다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/submissions"), tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"title":"링크 없는 제보","category":"축제","region":"SEOUL",
					 "beginDe":"2026-12-01T10:00:00","endDe":"2026-12-02T18:00:00"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 활동_제한_회원은_제보할_수_없다() throws Exception {
		mvc.perform(as(post("/api/v1/festivals/submissions"), tokenOf(MemberRole.ROLE_WARNING))
				.contentType("application/json")
				.content(submissionBody("제한 회원 제보")))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_제보할_수_없다() throws Exception {
		mvc.perform(post("/api/v1/festivals/submissions")
				.contentType("application/json")
				.content(submissionBody("비로그인 제보")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 내_제보_목록을_조회한다() throws Exception {
		mvc.perform(as(get("/api/v1/members/me/submissions"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void 미인증_사용자는_내_제보_목록을_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/members/me/submissions"))
			.andExpect(status().isUnauthorized());
	}

	/** 남의 제보는 존재 여부도 알려주지 않는다. */
	@Test
	void 남의_제보는_조회할_수_없다() throws Exception {
		mvc.perform(as(get("/api/v1/members/me/submissions/" + UUID.randomUUID()),
				tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isNotFound());
	}

	@Test
	void 없는_제보는_수정하거나_삭제할_수_없다() throws Exception {
		String me = tokenOf(MemberRole.ROLE_UNVERIFIED);
		UUID missing = UUID.randomUUID();

		mvc.perform(as(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.patch("/api/v1/members/me/submissions/" + missing), me)
				.contentType("application/json")
				.content(submissionBody("없는 제보 수정")))
			.andExpect(status().isNotFound());
		mvc.perform(as(delete("/api/v1/members/me/submissions/" + missing), me))
			.andExpect(status().isNotFound());
	}

	// ----- 날씨 -----

	@Test
	void 날씨는_비로그인도_조회한다() throws Exception {
		mvc.perform(get("/api/v1/weather?region=SEOUL&date=" + LocalDate.now()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.condition").isNotEmpty());
	}

	/** 단기예보 범위를 벗어나도 화면이 깨지지 않게 UNKNOWN 으로 돌려준다. */
	@Test
	void 예보_범위를_벗어난_날짜는_UNKNOWN이다() throws Exception {
		mvc.perform(get("/api/v1/weather?region=SEOUL&date=" + LocalDate.now().plusYears(1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.condition").value("UNKNOWN"));
	}

	@Test
	void 지역이_없으면_날씨를_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/weather?date=" + LocalDate.now()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 없는_지역을_주면_거부한다() throws Exception {
		mvc.perform(get("/api/v1/weather?region=NOPE&date=" + LocalDate.now()))
			.andExpect(status().isBadRequest());
	}

	// ----- 이미지 업로드 -----

	@Test
	void 이미지가_아닌_파일은_업로드할_수_없다() throws Exception {
		var notAnImage = new MockMultipartFile(
			"file", "bad.txt", "text/plain", "not an image".getBytes());

		mvc.perform(multipart("/api/v1/images").file(notAnImage))
			.andExpect(status().is4xxClientError());
	}

	// ----- 헬퍼 -----

	private UUID createPost() throws Exception {
		var result = mvc.perform(as(post("/api/v1/festivals/" + festivalId() + "/posts"),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content("""
					{"title":"좋아요테스트 후기 %s","content":"내용"}
					""".formatted(shortToken())))
			.andExpect(status().isOk())
			.andReturn();
		return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("id").asString());
	}

	private String submissionBody(String title) {
		return """
			{"title":"%s","category":"축제","referenceUrl":"https://example.com/%s",
			 "region":"SEOUL","beginDe":"2026-12-01T10:00:00","endDe":"2026-12-02T18:00:00"}
			""".formatted(title, shortToken());
	}

	private String shortToken() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
