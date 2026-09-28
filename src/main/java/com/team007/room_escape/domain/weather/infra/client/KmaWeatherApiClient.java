package com.team007.room_escape.domain.weather.infra.client;

import com.team007.room_escape.domain.weather.infra.dto.ShortTermForecastItem;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 기상청 API허브 단기예보(getVilageFcst) 호출. */
@Slf4j
@Component
@EnableConfigurationProperties(KmaWeatherProperties.class)
public class KmaWeatherApiClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10); // 10초 안에 응답 없으면 포기 (날씨는 부가기능이라 오래 붙잡고 있으면 안 됨)

    private final RestClient restClient; // 실제 HTTP 요청을 보내는 객체
    private final KmaWeatherProperties properties; //KmaWeatherProperties(url, serviceKey). 요청 URL 조립 시 사용
    private final ObjectMapper objectMapper; //JSON을 자바 객체로 변환하는 Jackson 도구 => Spring이 이미 빈으로 등록해둔 걸 생성자로 주입받음

    public KmaWeatherApiClient(KmaWeatherProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .build();
    }

    private SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) REQUEST_TIMEOUT.toMillis()); // 연결 자체가 안 될 때의 한도
        requestFactory.setReadTimeout((int) REQUEST_TIMEOUT.toMillis()); // 연결은 됐는데 응답이 안 올 때의 한도
        return requestFactory;
    }

    /** baseDate(yyyyMMdd)/baseTime(HHmm) 발표 회차 기준, 격자(nx, ny)의 예보 항목 전체를 받아온다. */
    public List<ShortTermForecastItem> fetchShortTerm(int nx, int ny, String baseDate, String baseTime) {
        // 응답이 {response:{body:{items:{item:[...]}}}} 처럼 여러 겹으로 감싸져 있어서,
        // 바로 받지 않고 JsonNode로 받아 필요한 부분만 받음
        JsonNode body = restClient.get()
            .uri(buildUri(nx, ny, baseDate, baseTime))
            .retrieve()
            .body(JsonNode.class);
        // .path()는 중간에 키가 없어도(에러 응답 등) 예외 대신 빈 값을 돌려줘서 여기서 앱이 죽지 않는다.
        JsonNode items = body.path("response").path("body").path("items").path("item");
        return objectMapper.convertValue(items, new TypeReference<List<ShortTermForecastItem>>() {
        });
    }

    private URI buildUri(int nx, int ny, String baseDate, String baseTime) {
        // URL을 넣으면 "//"가 "/"로 뭉개지는 버그->완성된 URL을 먼저 파싱하고 쿼리 파라미터만 뒤에 붙인다.
        return UriComponentsBuilder.fromUriString(properties.url())
            .queryParam("authKey", properties.serviceKey()) // data.go.kr의 serviceKey와 달리 이 API허브는 authKey
            .queryParam("dataType", "JSON")
            .queryParam("numOfRows", 1000) // 하루 8회 × 5일치 × 여러 category라 넉넉히 (300은 부족해서 뒷부분이 잘렸었음)
            .queryParam("pageNo", 1)
            .queryParam("base_date", baseDate)
            .queryParam("base_time", baseTime)
            .queryParam("nx", nx)
            .queryParam("ny", ny)
            .build()
            .toUri();
    }
}
