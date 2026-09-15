package com.team007.room_escape.domain.festival.infra.client;

import com.team007.room_escape.domain.festival.infra.dto.FestivalApiResult;
import com.team007.room_escape.domain.festival.infra.dto.FestivalApiRow;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@EnableConfigurationProperties(FestivalPublicApiProperties.class)
public class FestivalPublicApiClient {

    /** 30초 안에 응답 X -> 실패처리 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient = WebClient.builder().build();
    private final FestivalPublicApiProperties properties;
    private final ObjectMapper objectMapper;

    /** API 호출 담당*/
    public FestivalPublicApiClient(FestivalPublicApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties; //URL, 인증키, cron 값이 담긴 설정 객체
        this.objectMapper = objectMapper; //JSON 문자열을 자바 객체로 변경
    }

    /** pIndex (페이지), pSize (건 수)만큼 행사 데이터 요청*/
    public FestivalApiResult fetch(int pIndex, int pSize) {
        String body;
        try {
            body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(properties.url())
                    .queryParam("KEY", properties.serviceKey())
                    .queryParam("Type", "json")
                    .queryParam("pIndex", pIndex)
                    .queryParam("pSize", pSize)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT)
                .block();
        }  catch (Exception e) {
            log.error("[{}] 공공 API 호출 실패", FestivalExceptionCode.PUBLIC_API_CALL_FAILED.getCode(), e);
            throw new BusinessException(FestivalExceptionCode.PUBLIC_API_CALL_FAILED);
        }
        return parse(body);
    }

    /** 응답이 {"GGCULTUREVENTSTUS": [ {head: [...]}, {row: [...]} ]} 형태*/
    /** 근데 head, row도 정해진 키가 아니라 배열 순서로 섞여 있어서 순회하며 탐색*/
    private FestivalApiResult parse(String body) {
        try { JsonNode root = objectMapper.readTree(body).path("GGCULTUREVENTSTUS");

            int totalCount = 0;
            List<FestivalApiRow> rows = List.of();

            for (JsonNode section : root) {
                if (section.has("head")) {

                    /** head 안에도 list_total_count, RESULT, api_version이 낱개 객체로 흩어져 있어서 원하는 키를 가진 것만 찾는다*/
                    for (JsonNode headItem : section.get("head")) {
                        if (headItem.has("list_total_count")) {
                            totalCount = headItem.get("list_total_count").asInt();
                        }
                    }
                }
                if (section.has("row")) { rows = objectMapper.convertValue( section.get("row"),
                    new TypeReference<List<FestivalApiRow>>() {});
                }
            }return new FestivalApiResult(totalCount, rows);
        } catch (Exception e) {
            log.error("[{}] 공공 API 응답 파싱 실패", FestivalExceptionCode.PUBLIC_API_PARSE_FAILED.getCode(), e);
            throw new BusinessException(FestivalExceptionCode.PUBLIC_API_PARSE_FAILED);
        }
    }

}
