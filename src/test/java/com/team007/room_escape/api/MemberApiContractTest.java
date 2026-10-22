package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.global.util.CookieUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 회원 6개 + 내 콘텐츠 2개.
 *
 * 비밀번호 변경은 메일 인증 코드를 거쳐야 해서, 코드 발송까지만 확인하고
 * 인증·변경은 코드 없이 호출했을 때 거부되는지로 계약을 고정한다.
 */
class MemberApiContractTest extends ApiContractSupport {

	@Test
	void 내_정보를_조회한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(get("/api/v1/members/me"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(me.getId().toString()))
			.andExpect(jsonPath("$.data.email").value(me.getEmail()))
			.andExpect(jsonPath("$.data.nickname").value(me.getNickname()));
	}

	@Test
	void 미인증_사용자는_내_정보를_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/members/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 활동_제한_회원도_내_정보는_조회한다() throws Exception {
		Member restricted = saveMember(MemberRole.ROLE_WARNING);

		mvc.perform(as(get("/api/v1/members/me"), bearer(restricted)))
			.andExpect(status().isOk());
	}

	@Test
	void 닉네임을_수정한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);
		String nickname = "바꾼닉네임" + shortToken();

		mvc.perform(as(patch("/api/v1/members/me"), bearer(me))
				.contentType("application/json")
				.content("""
					{"nickname":"%s"}
					""".formatted(nickname)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.nickname").value(nickname));
	}

	@Test
	void 한_글자_닉네임으로는_수정할_수_없다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/members/me"), bearer(me))
				.contentType("application/json")
				.content("""
					{"nickname":"짧"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 남이_쓰는_닉네임으로는_수정할_수_없다() throws Exception {
		Member other = saveMember(MemberRole.ROLE_UNVERIFIED);
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/members/me"), bearer(me))
				.contentType("application/json")
				.content("""
					{"nickname":"%s"}
					""".formatted(other.getNickname())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER002"));
	}

	@Test
	void 미인증_사용자는_내_정보를_수정할_수_없다() throws Exception {
		mvc.perform(patch("/api/v1/members/me")
				.contentType("application/json")
				.content("""
					{"nickname":"비로그인"}
					"""))
			.andExpect(status().isUnauthorized());
	}

	/** 인증 코드 없이 바로 비밀번호를 바꿀 수는 없다. */
	@Test
	void 메일_인증_없이는_비밀번호를_바꿀_수_없다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(patch("/api/v1/members/me/password"), bearer(me))
				.contentType("application/json")
				.content("""
					{"newPassword":"NewPass1!"}
					"""))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 틀린_인증_코드는_거부한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(post("/api/v1/members/me/password/verify"), bearer(me))
				.contentType("application/json")
				.content("""
					{"code":"000000"}
					"""))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 미인증_사용자는_인증_코드를_받을_수_없다() throws Exception {
		mvc.perform(post("/api/v1/members/me/password/verification-code"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 비밀번호가_틀리면_탈퇴할_수_없다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(delete("/api/v1/members/me"), bearer(me))
				.contentType("application/json")
				.content("""
					{"password":"WrongPass1!"}
					"""))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 빈_비밀번호로는_탈퇴할_수_없다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(delete("/api/v1/members/me"), bearer(me))
				.contentType("application/json")
				.content("""
					{"password":""}
					"""))
			.andExpect(status().isBadRequest());
	}

	/** 탈퇴하면 소프트 삭제되고 브라우저의 리프레시 쿠키도 지운다. */
	@Test
	void 탈퇴하면_내_정보_조회가_막히고_쿠키가_만료된다() throws Exception {
		String email = signedUpEmail();
		Member me = memberRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
		String token = bearer(me);

		mvc.perform(as(delete("/api/v1/members/me"), token)
				.contentType("application/json")
				.content("""
					{"password":"%s"}
					""".formatted(SIGNUP_PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(cookie().maxAge(CookieUtil.REFRESH_TOKEN_COOKIE, 0));

		// 토큰은 아직 유효하지만 회원이 탈퇴 상태라 조회가 막힌다.
		mvc.perform(as(get("/api/v1/members/me"), token))
			.andExpect(status().is4xxClientError());
	}

	@Test
	void 미인증_사용자는_탈퇴할_수_없다() throws Exception {
		mvc.perform(delete("/api/v1/members/me")
				.contentType("application/json")
				.content("""
					{"password":"Password1!"}
					"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 내가_쓴_후기_목록을_조회한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(get("/api/v1/members/me/posts"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 내가_쓴_댓글_목록을_조회한다() throws Exception {
		Member me = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(as(get("/api/v1/members/me/comments"), bearer(me)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 미인증_사용자는_내_콘텐츠를_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/members/me/posts"))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/members/me/comments"))
			.andExpect(status().isUnauthorized());
	}

	private static final String SIGNUP_PASSWORD = "Password1!";

	/** 가입 API 로 만들어야 비밀번호가 인코딩되어 탈퇴까지 확인할 수 있다. */
	private String signedUpEmail() throws Exception {
		String email = "contract-" + UUID.randomUUID() + "@example.com";
		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content("""
					{"email":"%s","password":"%s","nickname":"탈퇴테스트%s"}
					""".formatted(email, SIGNUP_PASSWORD, shortToken())))
			.andExpect(status().isCreated());
		return email;
	}

	private String shortToken() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
