package com.team007.room_escape.global.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * 사용자가 작성한 글쓰기 에디터 HTML에서 화면 표시에 필요한 태그만 남긴다.
 */
@Component
public class RichTextSanitizer {

	private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
		.allowElements("p", "h2", "strong", "em", "ul", "ol", "li", "blockquote", "br")
		.allowUrlProtocols("http", "https")
		.allowElements("a")
		.allowAttributes("href").onElements("a")
		.requireRelNofollowOnLinks()
		.toFactory();

	public String sanitize(String content) {
		if (content == null) {
			return null;
		}

		return POLICY.sanitize(content).trim();
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
