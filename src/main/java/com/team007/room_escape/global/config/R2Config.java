package com.team007.room_escape.global.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Cloudflare R2 접속 설정.
 * R2는 S3 호환이라 AWS S3 SDK에 엔드포인트만 R2로 바꿔서 쓴다.
 */
@Configuration
@EnableConfigurationProperties(R2Properties.class)
@RequiredArgsConstructor
public class R2Config {

	private final R2Properties properties;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.endpointOverride(URI.create(properties.endpoint()))
			// R2는 리전 개념이 없어 auto를 쓴다. 다른 값을 넣으면 서명이 어긋난다.
			.region(Region.of("auto"))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
			.forcePathStyle(true)
			// AWS SDK 2.30+는 기본으로 CRC32 체크섬 헤더를 붙이는데 R2가 이를 거부한다.
			.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
			.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
			.build();
	}
}
