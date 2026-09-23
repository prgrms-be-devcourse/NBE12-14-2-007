package com.team007.room_escape.global.security;

import com.team007.room_escape.global.jwt.JwtAuthenticationFilter;
import com.team007.room_escape.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalReviewSearchSecurityTest {
    @ParameterizedTest
    @CsvSource({
        "default, GET, /api/v1/admin/posts?test=1, 127.0.0.1, 401",
        "local-review-search-test, GET, /api/v1/admin/posts?type=TITLE&keyword=test, 127.0.0.1, 200",
        "local-review-search-test, GET, /api/v1/admin/posts, ::1, 200",
        "local-review-search-test, GET, /api/v1/admin/posts, 192.168.1.20, 401",
        "local-review-search-test, POST, /api/v1/admin/posts, 127.0.0.1, 401",
        "local-review-search-test, DELETE, /api/v1/posts/11111111-1111-4111-8111-111111111111, 127.0.0.1, 401",
        "local-review-search-test, GET, /api/v1/posts/11111111-1111-4111-8111-111111111111, 127.0.0.1, 401",
        "local-review-search-test, GET, /api/v1/admin/members, 127.0.0.1, 401",
        "local-review-search-test, PATCH, /api/v1/admin/members/test/role, 127.0.0.1, 401"
    })
    void anonymousAccessIsLimitedToLocalSearch(String profile, String method, String path,
                                              String remoteAddress, int expectedStatus) throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().setActiveProfiles(profile);
            context.register(TestConfig.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(request(HttpMethod.valueOf(method), path).with(req -> {
                req.setRemoteAddr(remoteAddress);
                return req;
            })).andExpect(status().is(expectedStatus));
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, TestController.class})
    static class TestConfig {
        @Bean
        JwtProvider jwtProvider() { return mock(JwtProvider.class); }

        @Bean
        JwtAuthenticationEntryPoint authenticationEntryPoint() throws Exception {
            var entryPoint = mock(JwtAuthenticationEntryPoint.class);
            doAnswer(invocation -> {
                invocation.<HttpServletResponse>getArgument(1).setStatus(401);
                return null;
            }).when(entryPoint).commence(any(), any(), any());
            return entryPoint;
        }

        @Bean
        JwtAccessDeniedHandler accessDeniedHandler() throws Exception {
            var handler = mock(JwtAccessDeniedHandler.class);
            doAnswer(invocation -> {
                invocation.<HttpServletResponse>getArgument(1).setStatus(403);
                return null;
            }).when(handler).handle(any(), any(), any());
            return handler;
        }
    }

    @RestController
    static class TestController {
        @RequestMapping("/api/v1/**")
        String response() { return "ok"; }
    }
}
