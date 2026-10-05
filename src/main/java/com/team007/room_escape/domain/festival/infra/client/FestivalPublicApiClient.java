package com.team007.room_escape.domain.festival.infra.client;

import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 경기 문화행사 공공 API 호출·파싱. 배치에서 순차 호출만 해서 RestClient를 쓴다. */
@Slf4j
@Component
@EnableConfigurationProperties(FestivalPublicApiProperties.class)
public class FestivalPublicApiClient {

	/** 30초 안에 응답이 없으면 실패 처리 */
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	/** User-Agent가 없으면(Java 기본값 포함) 보안 장비가 200과 차단 HTML을 돌려준다. */
	private static final String USER_AGENT =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

	private final RestClient restClient;
	private final FestivalPublicApiProperties properties;
	private final ObjectMapper objectMapper;

	/** API 호출 담당 */
	public FestivalPublicApiClient(FestivalPublicApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties; /**URL, 인증키, cron 값이 담긴 설정 객체**/
		this.objectMapper = objectMapper; //JSON 문자열을 자바 객체로 변경
		// 타임아웃용 requestFactory를 먼저 조립해야 해서 생성자 안에서 만든다.
		this.restClient = RestClient.builder()
			.requestFactory(timeoutRequestFactory())
			.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
			.configureMessageConverters(converters ->
				converters.withJsonConverter(htmlTolerantJsonConverter()))
			.build();
	}

	/** 정상 응답도 Content-Type이 text/html이라 text/html을 JSON으로 읽는 컨버터로 바꾼다. */
	private JacksonJsonHttpMessageConverter htmlTolerantJsonConverter() {
		JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
		converter.setSupportedMediaTypes(List.of(
			MediaType.APPLICATION_JSON,
			MediaType.TEXT_HTML
		));
		return converter;
	}

	/** RestClient엔 .timeout()이 없어서 연결/응답 타임아웃을 requestFactory에 설정한다. */
	private SimpleClientHttpRequestFactory timeoutRequestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) REQUEST_TIMEOUT.toMillis());
		requestFactory.setReadTimeout((int) REQUEST_TIMEOUT.toMillis());
		return requestFactory;
	}

	/** pIndex(페이지), pSize(건 수)만큼 행사 데이터를 요청한다. */
	public FestivalApiResult fetch(int pIndex, int pSize) {
		// String으로 받아 다시 파싱하면 메모리를 두 번 써서 JsonNode로 바로 받는다.
		JsonNode body;
		try {
			body = restClient.get()
				.uri(buildUri(pIndex, pSize))
				.retrieve()
				.body(JsonNode.class);
		}  catch (Exception e) {
			log.error("[{}] 공공 API 호출 실패", FestivalExceptionCode.PUBLIC_API_CALL_FAILED.getCode(), e);
			throw new BusinessException(FestivalExceptionCode.PUBLIC_API_CALL_FAILED);
		}
		return parse(body);
	}

	/** url()은 전체 주소라 path()에 넣으면 "//"가 깨진다. fromUriString으로 파싱 후 쿼리만 붙인다. */
	private URI buildUri(int pIndex, int pSize) {
		return UriComponentsBuilder.fromUriString(properties.url())
			.queryParam("KEY", properties.serviceKey())
			.queryParam("Type", "json")
			.queryParam("pIndex", pIndex)
			.queryParam("pSize", pSize)
			.build()
			.toUri();
	}

	/** 응답의 head, row가 키가 아니라 배열 순서로 섞여 있어서 순회하며 찾는다. */
	private FestivalApiResult parse(JsonNode body) {
		try {
			JsonNode root = body.path("GGCULTUREVENTSTUS");

			int totalCount = 0;
			List<FestivalApiRow> rows = List.of();

			for (JsonNode section : root) {
				if (section.has("head")) {

					// head 안 낱개 객체 중 원하는 키를 가진 것만 찾는다.
					for (JsonNode headItem : section.get("head")) {
						if (headItem.has("list_total_count")) {
							totalCount = headItem.get("list_total_count").asInt();
						}
					}
				}
				if (section.has("row")) {
					// row는 이미 트리라 convertValue로 POJO 리스트만 만든다.
					rows = objectMapper.convertValue(section.get("row"),
						new TypeReference<List<FestivalApiRow>>() {});
				}
			}return new FestivalApiResult(totalCount, rows);
		} catch (Exception e) {
			log.error("[{}] 공공 API 응답 파싱 실패", FestivalExceptionCode.PUBLIC_API_PARSE_FAILED.getCode(), e);
			throw new BusinessException(FestivalExceptionCode.PUBLIC_API_PARSE_FAILED);
		}
	}

}
