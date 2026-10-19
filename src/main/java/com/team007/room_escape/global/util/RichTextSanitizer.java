package com.team007.room_escape.global.util;

import com.team007.room_escape.global.config.R2Properties;
import java.util.regex.Pattern;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * 사용자가 작성한 글쓰기 에디터 HTML에서 화면 표시에 필요한 태그만 남긴다.
 */
@Component
public class RichTextSanitizer {

	private final PolicyFactory policy;

	/**
	 * 본문 이미지는 우리 R2에 올린 것만 남긴다. 외부 주소를 허용하면 핫링크나 추적 픽셀을 심을 수 있다.
	 * src가 허용되지 않은 img는 속성이 하나도 남지 않아 태그째 지워진다.
	 */
	// TODO 본문에서 빠지거나 글이 삭제된 이미지를 R2에서 지울 기준을 정할 것 (지금은 R2에 계속 남는다)
	public RichTextSanitizer(R2Properties r2Properties) {
		String publicUrl = r2Properties.publicUrl();
		String prefix = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
		Pattern r2ImageUrl = Pattern.compile(Pattern.quote(prefix) + "[^\\s\"'<>]+");

		this.policy = new HtmlPolicyBuilder()
			.allowElements("p", "h2", "strong", "em", "ul", "ol", "li", "blockquote", "br")
			.allowUrlProtocols("http", "https")
			.allowElements("a")
			.allowAttributes("href").onElements("a")
			.requireRelNofollowOnLinks()
			.allowElements("img")
			.allowAttributes("src").matching(r2ImageUrl).onElements("img")
			.allowAttributes("alt").onElements("img")
			.toFactory();
	}

	public String sanitize(String content) {
		if (content == null) {
			return null;
		}

		return policy.sanitize(content).trim();
	}

	public boolean hasVisibleText(String sanitizedContent) {
		if (sanitizedContent == null) {
			return false;
		}

		String textOnly = sanitizedContent
			.replaceAll("<[^>]*>", "")
			.replace("&nbsp;", "")
			.replace("&#160;", "");

		return !textOnly.isBlank();
	}
}
