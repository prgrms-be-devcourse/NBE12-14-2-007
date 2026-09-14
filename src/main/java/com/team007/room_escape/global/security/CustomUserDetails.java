package com.team007.room_escape.global.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * SecurityContext에 들어가는 로그인 사용자.
 * JWT 필터가 클레임으로 만들며 password는 로그인 시에만 채우고, 토큰 복원 시에는 null이다.
 * 컨트롤러에서는 @AuthenticationPrincipal 로 id, nickname, role, profileImg를 꺼낸다.
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	private final UUID id;
	private final String nickname;
	private final String role;
	private final String profileImg;
	private final String password;

	public boolean isAdmin() {
		return "ROLE_ADMIN".equals(role);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return nickname;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
