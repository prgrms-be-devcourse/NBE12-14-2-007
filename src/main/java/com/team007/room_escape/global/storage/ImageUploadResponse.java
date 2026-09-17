package com.team007.room_escape.global.storage;

/**
 * 업로드 결과.
 *
 * @param key DB에 저장할 값. URL이 아니라 이 key를 저장한다.
 * @param url 화면에서 바로 쓸 수 있는 공개 URL
 */
public record ImageUploadResponse(
	String key,
	String url
) {

	public static ImageUploadResponse of(String key, String url) {
		return new ImageUploadResponse(key, url);
	}
}
