package com.team007.room_escape.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.team007.room_escape.global.config.R2Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageUrlResolverTest {

	private static final String PUBLIC_URL = "https://img.example.com";

	private final ImageUrlResolver resolver =
		new ImageUrlResolver(new R2Properties("account", "access", "secret", "bucket", PUBLIC_URL));

	@Test
	@DisplayName("key는 공개 URL로 바꾸고, 이미 URL인 값은 그대로 돌려준다")
	void resolve() {
		assertThat(resolver.resolve("posts/member/a.png")).isEqualTo(PUBLIC_URL + "/posts/member/a.png");
		assertThat(resolver.resolve("https://other.com/a.png")).isEqualTo("https://other.com/a.png");
		assertThat(resolver.resolve(null)).isNull();
		assertThat(resolver.resolve(" ")).isNull();
	}

	@Test
	@DisplayName("우리 공개 URL은 key로 되돌리고, 그 외 값은 건드리지 않는다")
	void toKey() {
		assertThat(resolver.toKey(PUBLIC_URL + "/posts/member/a.png")).isEqualTo("posts/member/a.png");
		assertThat(resolver.toKey("posts/member/a.png")).isEqualTo("posts/member/a.png");
		assertThat(resolver.toKey("https://other.com/a.png")).isEqualTo("https://other.com/a.png");
		assertThat(resolver.toKey(null)).isNull();
	}
}
