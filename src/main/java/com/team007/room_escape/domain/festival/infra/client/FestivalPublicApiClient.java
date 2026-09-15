package com.team007.room_escape.domain.festival.infra.client;

import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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
            .build();
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
                /** uri: 실제로 요청을 보낼 주소(URL). base path + 쿼리 파라미터(KEY, Type, pIndex, pSize)를 조립해서
                 "https://openapi.gg.go.kr/GGCULTUREVENTSTUS?KEY=...&Type=json&pIndex=1&pSize=100" 형태로 완성한다 **/
                .uri(uriBuilder -> uriBuilder
                    .path(properties.url())
                    .queryParam("KEY", properties.serviceKey())
                    .queryParam("Type", "json")
                    .queryParam("pIndex", pIndex)
                    .queryParam("pSize", pSize)
                    .build())
                .retrieve()
                .body(JsonNode.class);
        }  catch (Exception e) {
            log.error("[{}] 공공 API 호출 실패", FestivalExceptionCode.PUBLIC_API_CALL_FAILED.getCode(), e);
            throw new BusinessException(FestivalExceptionCode.PUBLIC_API_CALL_FAILED);
        }
        return parse(body);
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
