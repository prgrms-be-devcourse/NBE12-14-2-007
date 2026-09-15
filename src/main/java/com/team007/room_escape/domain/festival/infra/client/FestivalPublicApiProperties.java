package com.team007.room_escape.domain.festival.infra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

//application.yaml의 public-api.festival 설정 바인딩.
//API 호출에 필요한 URL, 인증키, 배치 주기
@ConfigurationProperties(prefix = "public-api.festival")
public record FestivalPublicApiProperties(
    String url,
    String serviceKey,
    String syncCron
) {
}
