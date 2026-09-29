package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.global.storage.ImageUrlResolver;
import com.team007.room_escape.global.storage.R2StorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 공공 행사 이미지 원본 URL을 다운로드해서 리사이즈한 뒤 R2에 올린다.
 * 실패해도 동기화 배치 자체가 깨지면 안 되므로, 어떤 예외든 원본 URL을 그대로 돌려주는 것으로 대체한다.
 */
@Slf4j
@Component
public class FestivalImageProcessor {

	private static final int MAX_WIDTH = 1200;
	private static final String DIRECTORY = "festivals";
	private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

	/** weserv.nl 프록시가 특정 이미지에서 무한 대기했던 적이 있어, 다운로드에도 반드시 타임아웃을 둔다. */
	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final R2StorageService r2StorageService;
	private final ImageUrlResolver imageUrlResolver;

	public FestivalImageProcessor(R2StorageService r2StorageService, ImageUrlResolver imageUrlResolver) {
		this.r2StorageService = r2StorageService;
		this.imageUrlResolver = imageUrlResolver;
		// RestClient는 타임아웃 설정용 requestFactory를 먼저 조립해야 해서 필드 초기화가 아니라 생성자 안에서 만든다
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) DOWNLOAD_TIMEOUT.toMillis());
		requestFactory.setReadTimeout((int) DOWNLOAD_TIMEOUT.toMillis());
		this.restClient = RestClient.builder()
			.requestFactory(requestFactory)
			.build();
	}

	/**
	 * 원본 URL을 리사이즈해서 R2에 올린 뒤, 새 공개 URL을 돌려준다.
	 * 다운로드·리사이즈·업로드 중 어디서 실패하든(타임아웃 포함) 원본 URL을 그대로 돌려준다.
	 */
	public String process(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			return originalUrl;
		}
		try {
			byte[] original = restClient.get()
				.uri(originalUrl)
				.retrieve()
				.body(byte[].class);

			byte[] resized = resize(original);
			String key = r2StorageService.upload(resized, OUTPUT_CONTENT_TYPE, DIRECTORY);
			return imageUrlResolver.resolve(key);
		} catch (Exception e) {
			log.warn("행사 이미지 리사이즈 실패, 원본 URL 유지: {}", originalUrl, e);
			return originalUrl;
		}
	}

	/** 원본이 png든 webp든 jpg로 통일해서 저장한다 (인코딩 포맷을 여러 개 지원할 필요는 없어서). */
	private byte[] resize(byte[] original) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Thumbnails.of(new ByteArrayInputStream(original))
			.width(MAX_WIDTH) // 높이는 안 정해줘도 원본 비율 유지해서 자동 계산됨
			.outputFormat("jpg")
			.toOutputStream(out);
		return out.toByteArray();
	}
}
