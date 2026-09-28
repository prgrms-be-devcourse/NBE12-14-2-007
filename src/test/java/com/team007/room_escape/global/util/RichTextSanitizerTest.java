package com.team007.room_escape.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RichTextSanitizerTest {

	private final RichTextSanitizer sanitizer = new RichTextSanitizer();

	@Test
	void 허용한_서식은_유지한다() {
		String content = "<h2>행사 후기</h2><p><strong>즐거웠어요.</strong></p>"
			+ "<ul><li>준비물</li></ul>";

		assertThat(sanitizer.sanitize(content)).isEqualTo(content);
	}

	@Test
	void 스크립트와_이벤트_속성은_제거한다() {
		String content = "<p onclick=\"alert(1)\">안전한 내용</p>"
			+ "<script>alert(2)</script>";

		assertThat(sanitizer.sanitize(content))
			.isEqualTo("<p>안전한 내용</p>");
	}

	@Test
	void 자바스크립트_링크는_제거한다() {
		String content = "<p><a href=\"javascript:alert(1)\">위험한 링크</a></p>";

		assertThat(sanitizer.sanitize(content))
			.doesNotContain("javascript:")
			.contains("위험한 링크");
	}

	@Test
	void 태그만_남은_본문은_내용이_없는_것으로_판단한다() {
		assertThat(sanitizer.hasVisibleText("<p><br></p>")).isFalse();
		assertThat(sanitizer.hasVisibleText("<h2>소제목</h2>")).isTrue();
	}
}
