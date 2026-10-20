package com.team007.room_escape.domain.festival.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yaml의 public-api.culture-info 설정 바인딩 (URL, 인증키)
@ConfigurationProperties(prefix = "public-api.culture-info")
public record FestivalPublicApiPropertiesV2(
	String url,
	String serviceKey
) {
}
