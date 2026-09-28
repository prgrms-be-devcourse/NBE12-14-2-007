package com.team007.room_escape.global.security;

import com.team007.room_escape.global.jwt.JwtAuthenticationFilter;
import com.team007.room_escape.global.jwt.JwtProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정.
 * JWT 무상태 세션, /api/v1/auth와 스웨거만 공개, 나머지는 인증 필요.
 * @PreAuthorize 메서드 보안과 BCrypt, CORS도 여기서 연다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, AuthCookieProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint authenticationEntryPoint;
	private final JwtAccessDeniedHandler accessDeniedHandler;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/auth/**").permitAll()
				// 회원가입 화면에서 프로필 이미지를 올리려면 토큰이 없는 상태로도 업로드가 돼야 한다.
				// TODO 누구나 호출할 수 있어 R2 용량을 소진시키는 남용이 가능하다.
				//      가입 화면 외의 용도가 늘어나기 전에 업로드 제한(IP별 횟수 등)을 붙일 것.
				.requestMatchers(HttpMethod.POST, "/api/v1/images").permitAll()
				// TODO url 한꺼번에 정리하기 (지금은 공개 API가 늘어날 때마다 규칙을 한 줄씩 추가하고 있음)
				.requestMatchers(HttpMethod.GET, "/api/v1/festivals/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/posts").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/weather").permitAll()
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers(
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html"
				).permitAll()
				// Prometheus가 토큰 없이 긁어갈 수 있어야 한다.
				// TODO 운영 배포 시에는 관리 포트 분리나 IP 제한으로 외부 노출을 막을 것.
				.requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
				.anyRequest().authenticated()
			)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * 권한 계층. MemberRole enum의 선언 순서와 항상 동일하게 유지할 것.
	 * 상위 등급은 하위 등급의 권한을 모두 포함한다.
	 */
	@Bean
	static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.withDefaultRolePrefix()
				.role("ADMIN").implies("TRUSTED")
				.role("TRUSTED").implies("RECOGNIZED")
				.role("RECOGNIZED").implies("UNVERIFIED")
				.role("UNVERIFIED").implies("WARNING")
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(
			"http://localhost:3000",
			"http://localhost:3001"
		));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Authorization"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
