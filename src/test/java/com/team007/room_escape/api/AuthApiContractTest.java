package com.team007.room_escape.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import com.team007.room_escape.global.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** POST /auth/signup, /auth/login, /auth/refresh, /auth/logout */
class AuthApiContractTest extends ApiContractSupport {

	private static final String PASSWORD = "Password1!";

	@Test
	void 가입하면_201과_빈_본문을_받는다() throws Exception {
		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content(signupBody(uniqueEmail(), "가입테스트" + shortToken())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void 비밀번호_규칙에_맞지_않으면_거부한다() throws Exception {
		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content("""
					{"email":"%s","password":"short","nickname":"%s"}
					""".formatted(uniqueEmail(), "규칙테스트" + shortToken())))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 이미_쓰는_이메일로는_가입할_수_없다() throws Exception {
		Member existing = saveMember(MemberRole.ROLE_UNVERIFIED);

		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content(signupBody(existing.getEmail(), "중복테스트" + shortToken())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER001"));
	}

	/** 탈퇴 회원의 이메일은 보관 기간 동안 재가입을 막는다. 쓰는 중인 이메일과 안내를 구분한다. */
	@Test
	void 탈퇴한_이메일로는_재가입할_수_없다() throws Exception {
		Member withdrawn = saveMember(MemberRole.ROLE_UNVERIFIED);
		withdrawn.delete();

		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content(signupBody(withdrawn.getEmail(), "재가입테스트" + shortToken())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MEMBER010"));
	}

	@Test
	void 로그인하면_액세스_토큰과_리프레시_쿠키를_받는다() throws Exception {
		String email = signedUpEmail();

		mvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content(loginBody(email, PASSWORD)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(cookie().exists(CookieUtil.REFRESH_TOKEN_COOKIE))
			.andExpect(cookie().httpOnly(CookieUtil.REFRESH_TOKEN_COOKIE, true));
	}

	@Test
	void 비밀번호가_틀리면_로그인할_수_없다() throws Exception {
		String email = signedUpEmail();

		mvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content(loginBody(email, "WrongPass1!")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 리프레시_쿠키가_없으면_재발급할_수_없다() throws Exception {
		mvc.perform(post("/api/v1/auth/refresh"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 위조된_리프레시_토큰은_거부한다() throws Exception {
		mvc.perform(post("/api/v1/auth/refresh")
				.cookie(new Cookie(CookieUtil.REFRESH_TOKEN_COOKIE, "not-a-token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 리프레시하면_새_액세스_토큰을_받는다() throws Exception {
		Cookie refresh = loginAndGetRefreshCookie(signedUpEmail());

		mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
	}

	/**
	 * 등급이 내려간 뒤 재발급하면 낡은 등급이 아니라 현재 등급이 담겨야 한다.
	 * 리프레시 토큰에도 role 클레임이 들어 있어, 그 값을 재사용하도록 바뀌면
	 * 강등된 회원이 14일간 옛 권한을 유지하게 된다.
	 */
	@Test
	void 등급이_강등되면_재발급_토큰도_낮은_등급이다() throws Exception {
		String email = signedUpEmail();
		Cookie refresh = loginAndGetRefreshCookie(email);
		demoteToWarning(email);

		MvcResult result = mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
			.andExpect(status().isOk())
			.andReturn();

		assertRoleClaim(readAccessToken(result), MemberRole.ROLE_WARNING);
	}

	/** 탈퇴하면 리프레시 토큰이 살아 있어도 재발급하지 않는다. */
	@Test
	void 탈퇴한_회원은_재발급할_수_없다() throws Exception {
		String email = signedUpEmail();
		Cookie refresh = loginAndGetRefreshCookie(email);
		findByEmail(email).delete();

		mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 로그아웃하면_리프레시_쿠키가_만료된다() throws Exception {
		String email = signedUpEmail();
		Cookie refresh = loginAndGetRefreshCookie(email);

		mvc.perform(as(post("/api/v1/auth/logout"), bearer(findByEmail(email)))
				.cookie(refresh))
			.andExpect(status().isNoContent())
			.andExpect(cookie().maxAge(CookieUtil.REFRESH_TOKEN_COOKIE, 0));
	}

	/**
	 * /api/v1/auth/** 는 permitAll 이고 로그아웃은 쿠키를 지우는 멱등 연산이다.
	 * 토큰이 만료된 뒤에도 쿠키를 정리할 수 있어야 해서 인증을 요구하지 않는다.
	 */
	@Test
	void 로그아웃은_인증_없이도_호출된다() throws Exception {
		mvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isNoContent());
	}

	private String uniqueEmail() {
		return "contract-" + UUID.randomUUID() + "@example.com";
	}

	private String shortToken() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	/** 가입 API 를 거쳐 만든다. 비밀번호가 인코딩되어야 로그인까지 확인할 수 있다. */
	private String signedUpEmail() throws Exception {
		String email = uniqueEmail();
		mvc.perform(post("/api/v1/auth/signup")
				.contentType("application/json")
				.content(signupBody(email, "로그인테스트" + shortToken())))
			.andExpect(status().isCreated());
		return email;
	}

	private Cookie loginAndGetRefreshCookie(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content(loginBody(email, PASSWORD)))
			.andExpect(status().isOk())
			.andReturn();
		Cookie cookie = result.getResponse().getCookie(CookieUtil.REFRESH_TOKEN_COOKIE);
		if (cookie == null) {
			throw new IllegalStateException("로그인 응답에 리프레시 쿠키가 없다");
		}
		return cookie;
	}

	private String signupBody(String email, String nickname) {
		return """
			{"email":"%s","password":"%s","nickname":"%s"}
			""".formatted(email, PASSWORD, nickname);
	}

	private String loginBody(String email, String password) {
		return """
			{"email":"%s","password":"%s"}
			""".formatted(email, password);
	}

	private Member findByEmail(String email) {
		return memberRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
	}

	/** 관리자 API 를 쓰지 않고 DB 상태만 바꿔, 재발급이 DB 를 다시 읽는지 확인한다. */
	private void demoteToWarning(String email) {
		findByEmail(email).changeRole(MemberRole.ROLE_WARNING);
		entityManager.flush();
	}

	private String readAccessToken(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data").path("accessToken").asString();
	}

	private void assertRoleClaim(String accessToken, MemberRole expected) {
		assertThat(jwtProvider.parse(accessToken).get("role", String.class))
			.isEqualTo(expected.name());
	}
}
