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

/** 공공 행사 이미지를 리사이즈해 R2에 올린다. 실패하면 원본 URL을 그대로 돌려준다. */
@Slf4j
@Component
public class FestivalImageProcessor {

	private static final int MAX_WIDTH = 1200;
	private static final String DIRECTORY = "festivals";
	private static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

	/** weserv.nl 프록시가 무한 대기한 적이 있어 다운로드에도 타임아웃을 둔다. */
	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(10);

	private final RestClient restClient;
	private final R2StorageService r2StorageService;
	private final ImageUrlResolver imageUrlResolver;

	public FestivalImageProcessor(R2StorageService r2StorageService, ImageUrlResolver imageUrlResolver) {
		this.r2StorageService = r2StorageService;
		this.imageUrlResolver = imageUrlResolver;
		// 타임아웃용 requestFactory를 먼저 조립해야 해서 생성자 안에서 만든다.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) DOWNLOAD_TIMEOUT.toMillis());
		requestFactory.setReadTimeout((int) DOWNLOAD_TIMEOUT.toMillis());
		this.restClient = RestClient.builder()
			.requestFactory(requestFactory)
			.build();
	}

	/** 리사이즈해 R2에 올리고 새 URL을 돌려준다. 어디서 실패하든 원본 URL을 돌려준다. */
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

	/** 원본 포맷과 상관없이 jpg로 통일해서 저장한다. */
	private byte[] resize(byte[] original) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Thumbnails.of(new ByteArrayInputStream(original))
			.width(MAX_WIDTH) // 높이는 안 정해줘도 원본 비율 유지해서 자동 계산됨
			.outputFormat("jpg")
			.toOutputStream(out);
		return out.toByteArray();
	}
}
