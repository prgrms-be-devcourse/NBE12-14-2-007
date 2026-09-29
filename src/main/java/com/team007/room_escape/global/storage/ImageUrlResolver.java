package com.team007.room_escape.global.storage;

import com.team007.room_escape.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 저장 key를 브라우저가 접근 가능한 공개 URL로 바꾼다.
 *
 * DB에는 key("posts/abc.png")만 저장하고 도메인은 설정으로 둔다.
 * 그래야 r2.dev에서 커스텀 도메인으로 갈아탈 때 데이터를 건드리지 않아도 되고,
 * 같은 데이터로 로컬/운영이 각자 다른 URL을 쓸 수 있다.
 *
 * R2에 접근하지 않는 순수 변환기라서, 이미지 URL이 필요한 도메인 서비스는
 * R2StorageService(업로드·삭제) 대신 이 컴포넌트만 주입받으면 된다.
 */
@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

	private final R2Properties properties;

	/**
	 * @param key 저장 key. null이거나 비어 있으면 null을 돌려준다(이미지 없음).
	 *            key 도입 전에 URL 그대로 저장된 값이나 외부 원본 URL은 그대로 돌려준다
	 */
	public String resolve(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (isUrl(key)) {
			return key;
		}
		return "%s/%s".formatted(prefix(), key);
	}

	/**
	 * 요청으로 들어온 이미지 값을 저장용 key로 되돌린다.
	 * 화면은 응답에서 받은 URL을 그대로 다시 보내기도 해서, 우리 공개 URL이면 앞부분을 떼어낸다.
	 */
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
