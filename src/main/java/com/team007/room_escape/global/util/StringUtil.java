package com.team007.room_escape.global.util;

/**
 * 문자열 정규화.
 *
 * 빈 문자열과 null이 섞이면 "값 없음"이 두 가지로 표현되어 비교·검색이 어긋난다.
 * 저장 직전에 한 가지(null)로 모은다.
 *
 * 이름을 StringUtils가 아니라 StringUtil로 둔 이유는
 * org.springframework.util.StringUtils와 헷갈리지 않게 하기 위함이다.
 */
public final class StringUtil {

	private StringUtil() {
	}

	/** null이거나 공백뿐이면 null, 아니면 그대로 돌려준다. */
	public static String emptyToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}
}
