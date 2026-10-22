package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.festival.infra.entity.Festival;
import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** GET /admin/festivals, PATCH /admin/festivals/{id} + 제거된 레거시 경로 */
class AdminFestivalApiContractTest extends ApiContractSupport {

	@Autowired
	private FestivalRepository festivalRepository;

	@Test
	void 관리자는_행사_목록을_조회한다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/festivals"), tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").isArray());
	}

	@Test
	void 일반_회원은_행사_목록을_조회할_수_없다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/festivals"), tokenOf(MemberRole.ROLE_UNVERIFIED)))
			.andExpect(status().isForbidden());
	}

	@Test
	void 미인증_사용자는_행사_목록을_조회할_수_없다() throws Exception {
		mvc.perform(get("/api/v1/admin/festivals"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void 허용되지_않은_정렬_필드는_거부한다() throws Exception {
		mvc.perform(as(get("/api/v1/admin/festivals?sort=deletedAt,desc"),
				tokenOf(MemberRole.ROLE_ADMIN)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 관리자는_행사를_수정한다() throws Exception {
		Festival festival = saveFestival();

		mvc.perform(as(patch("/api/v1/admin/festivals/" + festival.getId()),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content(updateBody("수정된 행사", "2026-11-01T10:00:00", "2026-11-02T18:00:00")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("수정된 행사"));
	}

	@Test
	void 종료일이_시작일보다_앞서면_거부한다() throws Exception {
		Festival festival = saveFestival();

		mvc.perform(as(patch("/api/v1/admin/festivals/" + festival.getId()),
				tokenOf(MemberRole.ROLE_ADMIN))
				.contentType("application/json")
				.content(updateBody("기간 역전", "2026-11-05T10:00:00", "2026-11-01T18:00:00")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 일반_회원은_행사를_수정할_수_없다() throws Exception {
		Festival festival = saveFestival();

		mvc.perform(as(patch("/api/v1/admin/festivals/" + festival.getId()),
				tokenOf(MemberRole.ROLE_UNVERIFIED))
				.contentType("application/json")
				.content(updateBody("수정 시도", "2026-11-01T10:00:00", "2026-11-02T18:00:00")))
			.andExpect(status().isForbidden());
	}

	/** 삭제·복구는 /admin/deletions 로 옮겼다. 옛 경로가 되살아나면 이력 없이 지워진다. */
	@Test
	void 행사_삭제와_복구의_옛_경로는_살아있지_않다() throws Exception {
		Festival festival = saveFestival();
		String admin = tokenOf(MemberRole.ROLE_ADMIN);

		mvc.perform(as(delete("/api/v1/admin/festivals/" + festival.getId()), admin))
			.andExpect(status().isMethodNotAllowed());
		mvc.perform(as(patch("/api/v1/admin/festivals/" + festival.getId() + "/restore"), admin))
			.andExpect(status().isNotFound());
	}

	private Festival saveFestival() {
		return festivalRepository.saveAndFlush(Festival.builder()
			.providerType(ProviderType.PUBLIC)
			.title("계약테스트 행사 " + UUID.randomUUID().toString().substring(0, 8))
			.region(FestivalRegion.SEOUL)
			.beginDe(LocalDateTime.of(2026, 10, 1, 10, 0))
			.endDe(LocalDateTime.of(2026, 10, 5, 18, 0))
			.build());
	}

	private String updateBody(String title, String beginDe, String endDe) {
		return """
			{"title":"%s","category":"축제","region":"SEOUL",
			 "beginDe":"%s","endDe":"%s"}
			""".formatted(title, beginDe, endDe);
	}
}
