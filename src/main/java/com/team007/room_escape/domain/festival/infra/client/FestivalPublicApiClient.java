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

/**
 * 경기 문화행사 공공 API(GGCULTUREVENTSTUS) 호출 및 응답 파싱 담당.
 * 스케줄러(배치)에서 순차 호출만 하면 되는 동기 흐름이라 WebClient 대신 RestClient를 사용한다.
 */
@Slf4j
@Component
@EnableConfigurationProperties(FestivalPublicApiProperties.class)
public class FestivalPublicApiClient {

    /** 30초 안에 응답 X -> 실패처리 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 경기도 공공 API 앞단의 보안 장비가 브라우저가 아닌 요청을 막는다.
     * User-Agent 를 안 보내면 HTTP 200 에 "보안 정책에 의해 차단 되었습니다" HTML 이 돌아온다.
     * (Java 기본값인 "Java/25" 도 차단된다)
     */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private final RestClient restClient;
    private final FestivalPublicApiProperties properties;
    private final ObjectMapper objectMapper;

    /** API 호출 담당 **/
    public FestivalPublicApiClient(FestivalPublicApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties; /**URL, 인증키, cron 값이 담긴 설정 객체**/
        this.objectMapper = objectMapper; //JSON 문자열을 자바 객체로 변경
        /** RestClient는 타임아웃 설정용 requestFactory를 먼저 조립해야 해서 필드 초기화가 아니라 생성자 안에서 만든다 **/
        this.restClient = RestClient.builder()
            .requestFactory(timeoutRequestFactory())
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .configureMessageConverters(converters ->
                converters.withJsonConverter(htmlTolerantJsonConverter()))
            .build();
    }

    /**
     * 이 API는 정상 응답도 Content-Type 을 text/html 로 내려준다. 본문은 JSON 인데 헤더만 틀린 것이다.
     * 기본 Jackson 컨버터는 application/json 만 처리해서
     * "no suitable HttpMessageConverter found ... content type [text/html]" 로 실패한다.
     * 그래서 text/html 도 JSON 으로 읽는 컨버터로 갈아 끼운다.
     *
     * 주입받은 objectMapper 대신 기본 생성자를 쓰는 이유: 이 컨버터는 JsonMapper 를 받는데
     * 여기서 하는 일은 응답을 JsonNode 트리로 읽는 것뿐이라 매퍼 설정이 결과에 영향을 주지 않는다.
     * 실제 POJO 변환은 아래 parse() 에서 주입받은 objectMapper 로 한다.
     */
    private JacksonJsonHttpMessageConverter htmlTolerantJsonConverter() {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_HTML
        ));
        return converter;
    }

    /**
     * RestClient는 WebClient의 .timeout() 같은 체이닝 연산자가 없어서,
     * 연결/응답 타임아웃을 HTTP 요청 자체를 담당하는 ClientHttpRequestFactory에 미리 설정해둔다.
     */
    private SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) REQUEST_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) REQUEST_TIMEOUT.toMillis());
        return requestFactory;
    }

    /** pIndex (페이지), pSize (건 수)만큼 행사 데이터 요청 **/
    public FestivalApiResult fetch(int pIndex, int pSize) {
        /** 응답을 String으로 받아 objectMapper.readTree(String)로 다시 파싱하면
        문자열 전체를 한 번 더 메모리에 만드는 셈이라, 처음부터 JsonNode로 바로 받는다 **/
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

    /**
     * 요청 주소를 조립한다.
     * "https://openapi.gg.go.kr/GGCULTUREVENTSTUS?KEY=...&Type=json&pIndex=1&pSize=100"
     *
     * properties.url()은 스킴과 호스트까지 들어 있는 전체 주소다.
     * 이걸 uriBuilder.path()에 넣으면 경로 조각으로 취급돼 "//"가 "/"로 줄어든다.
     * 그러면 "https:/openapi.gg.go.kr/..."가 되어 호스트가 사라지고
     * "protocol = https host = null"로 연결 자체가 실패한다.
     * 그래서 전체 주소를 fromUriString으로 파싱한 뒤 쿼리만 덧붙인다.
     */
    private URI buildUri(int pIndex, int pSize) {
        return UriComponentsBuilder.fromUriString(properties.url())
            .queryParam("KEY", properties.serviceKey())
            .queryParam("Type", "json")
            .queryParam("pIndex", pIndex)
            .queryParam("pSize", pSize)
            .build()
            .toUri();
    }

    /** 응답이 {"GGCULTUREVENTSTUS": [ {head: [...]}, {row: [...]} ]} 형태
    근데 head, row도 정해진 키가 아니라 배열 순서로 섞여 있어서 순회하며 탐색 **/
    private FestivalApiResult parse(JsonNode body) {
        try {
            JsonNode root = body.path("GGCULTUREVENTSTUS");

            int totalCount = 0;
            List<FestivalApiRow> rows = List.of();

            for (JsonNode section : root) {
                if (section.has("head")) {

                    /** head 안에도 list_total_count, RESULT, api_version이 낱개 객체로 흩어져 있어서 원하는 키를 가진 것만 찾는다 **/
                    for (JsonNode headItem : section.get("head")) {
                        if (headItem.has("list_total_count")) {
                            totalCount = headItem.get("list_total_count").asInt();
                        }
                    }
                }
                if (section.has("row")) {
                    /** row는 이미 JsonNode 트리 상태라 readTree는 필요 없고, 트리 -> POJO 리스트 변환만 하면 되므로 convertValue를 그대로 사용한다 **/
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
