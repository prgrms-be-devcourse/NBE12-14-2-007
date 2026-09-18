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
	 * @param key 저장 key. null이거나 비어 있으면 null을 돌려준다(이미지 없음)
	 */
	public String resolve(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		return "%s/%s".formatted(properties.publicUrl(), key);
	}
}
