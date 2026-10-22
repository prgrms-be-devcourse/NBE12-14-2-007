package com.team007.room_escape.domain.deletion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team007.room_escape.domain.festival.controller.AdminFestivalController;
import com.team007.room_escape.domain.festival.service.AdminFestivalService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 경로 제거 검증이다. 보안 필터를 포함한 인증·인가 통합 테스트는 아니다. */
class LegacyAdminDeletionRouteTest {
	@Test
	void legacyFestivalDeletionAndRestorationRoutesAreNotAvailable() throws Exception {
		var service = mock(AdminFestivalService.class);
		var mvc = MockMvcBuilders.standaloneSetup(new AdminFestivalController(service)).build();
		mvc.perform(delete("/api/v1/admin/festivals/42")).andExpect(status().isMethodNotAllowed());
		mvc.perform(patch("/api/v1/admin/festivals/42/restore")).andExpect(status().isNotFound());
		verifyNoInteractions(service);
	}
}
