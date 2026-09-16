package com.team007.room_escape.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 r2.* 설정 바인딩.
 * Cloudflare R2 접속 정보와 공개 URL의 앞부분을 담는다.
 */
@ConfigurationProperties(prefix = "r2")
public record R2Properties(
	String accountId,
	String accessKey,
	String secretKey,
	String bucket,
	String publicUrl
) {

	/** R2의 S3 호환 엔드포인트. 계정마다 주소가 다르다. */
	public String endpoint() {
		return "https://%s.r2.cloudflarestorage.com".formatted(accountId);
	}
}
