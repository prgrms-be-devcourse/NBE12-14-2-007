package com.team007.room_escape.global.storage;

import com.team007.room_escape.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 저장 key를 공개 URL로 바꾼다. R2에 접근하지 않아서 URL만 필요한 서비스는 이것만 주입받으면 된다. */
@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

	private final R2Properties properties;

	/** 비어 있으면 null, 이미 URL로 저장된 옛 값이나 외부 URL은 그대로 돌려준다. */
	public String resolve(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (isUrl(key)) {
			return key;
		}
		return "%s/%s".formatted(prefix(), key);
	}

	/** 요청으로 들어온 이미지 값을 key로 되돌린다. 화면이 받은 URL을 그대로 보내기도 해서 앞부분을 뗀다. */
	public String toKey(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String prefix = prefix() + "/";
		return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
	}

	private boolean isUrl(String value) {
		return value.startsWith("http://") || value.startsWith("https://");
	}

	private String prefix() {
		String publicUrl = properties.publicUrl();
		return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
	}
}
