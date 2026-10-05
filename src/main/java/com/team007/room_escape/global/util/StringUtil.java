package com.team007.room_escape.global.util;

/** 빈 문자열과 null을 null 하나로 모은다. Spring의 StringUtils와 헷갈리지 않게 StringUtil로 지었다. */
public final class StringUtil {

	private StringUtil() {
	}

	/** null이거나 공백뿐이면 null, 아니면 그대로 돌려준다. */
	public static String emptyToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}
}
