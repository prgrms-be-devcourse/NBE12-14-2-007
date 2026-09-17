package com.team007.room_escape.global.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 업로드 대상 구분. 버킷 안에서 어느 폴더에 저장할지를 정한다.
 * 클라이언트가 임의 경로를 넘기지 못하도록 enum으로 고정한다.
 */
@Getter
@RequiredArgsConstructor
public enum ImageType {

	POST("posts"),
	PROFILE("profiles"),
	INQUIRY("inquiries"),
	FESTIVAL("festivals");

	private final String directory;
}
