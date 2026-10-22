package com.team007.room_escape.domain.festival.infra.client;

import com.team007.room_escape.domain.festival.infra.dto.CultureInfoDetailRow;
import com.team007.room_escape.domain.festival.infra.dto.CultureInfoPeriodRow;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.xml.XmlMapper;

/** 전국 문화정보 API(period2/detail2) 호출·파싱. */
@Slf4j
@Component
@EnableConfigurationProperties(FestivalPublicApiPropertiesV2.class)
public class FestivalPublicApiClientV2 {

	/** 요청 응답 시간이 30초 이상일 경우 포기 **/
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

	/** 실제로 HTTP 요청을 날리는 스프링 도구. 한 번 만들어두고 모든 요청에 재사용해 (요청마다 새로 안 만듦) **/
	private final RestClient restClient;
	/**a pplication.yaml에서 읽어온 url/serviceKey. **/
	private final FestivalPublicApiPropertiesV2 properties;
	/** XML 텍스트를 자바 객체로 바꿔주는 도구 **/
	private final XmlMapper xmlMapper = new XmlMapper();

	public FestivalPublicApiClientV2(FestivalPublicApiPropertiesV2 properties) {
		this.properties = properties;
		this.restClient = RestClient.builder()
			.requestFactory(timeoutRequestFactory())
			.build();
	}

	private SimpleClientHttpRequestFactory timeoutRequestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout((int) REQUEST_TIMEOUT.toMillis()); //연결 타임아웃(connectTimeout): 서버에 연결 자체가 안 될 때 얼마나 기다릴지
		requestFactory.setReadTimeout((int) REQUEST_TIMEOUT.toMillis()); //읽기 타임아웃(readTimeout): 연결은 됐는데 응답이 안 올 때 얼마나 기다릴지
		return requestFactory;
	}

	/** 기간 내 행사 목록을 받아온다 (전체 타입: 공연/전시, 행사/축제, 교육/체험). 상세 정보는 없고 seq/title만 들어있다. */
	public List<CultureInfoPeriodRow> fetchPeriod(int pageNo, int numOfRows, String from, String to) {
		String xml = callApi(buildPeriodUri(pageNo, numOfRows, from, to));
		return parseItems(xml, CultureInfoPeriodRow.class);
	}

	/** 신규로 판별된 행사 하나의 상세 정보를 받아온다. 없으면 null. */
	public CultureInfoDetailRow fetchDetail(String seq) {
		String xml = callApi(buildDetailUri(seq));
		List<CultureInfoDetailRow> items = parseItems(xml, CultureInfoDetailRow.class);
		return items.isEmpty() ? null : items.get(0);
	}

	private String callApi(URI uri) {
		try {
			// Content-Type에 charset이 없어서 문자열로 바로 받으면 한글이 깨진다. 바이트로 받아 UTF-8로 직접 디코딩한다.
			byte[] body = restClient.get().uri(uri).retrieve().body(byte[].class);
			return body == null ? null : new String(body, StandardCharsets.UTF_8);
		} catch (Exception e) {
			log.error("[{}] 전국 문화정보 API 호출 실패", FestivalExceptionCode.PUBLIC_API_CALL_FAILED.getCode(), e);
			throw new BusinessException(FestivalExceptionCode.PUBLIC_API_CALL_FAILED);
		}
	}

	/** item이 1건뿐이면 배열이 아니라 단일 객체로 오기도 해서, 배열/단일 둘 다 받아들이게 처리한다. */
	private <T> List<T> parseItems(String xml, Class<T> rowType) {
		try {
			JsonNode root = xmlMapper.readTree(xml);
			JsonNode itemNode = root.findPath("item");
			if (itemNode.isMissingNode()) {
				return List.of();
			}
			if (itemNode.isArray()) {
				List<T> rows = new ArrayList<>();
				itemNode.forEach(node -> rows.add(xmlMapper.convertValue(node, rowType)));
				return rows;
			}
			return List.of(xmlMapper.convertValue(itemNode, rowType));
		} catch (Exception e) {
			log.error("[{}] 전국 문화정보 API 응답 파싱 실패", FestivalExceptionCode.PUBLIC_API_PARSE_FAILED.getCode(), e);
			throw new BusinessException(FestivalExceptionCode.PUBLIC_API_PARSE_FAILED);
		}
	}

	private URI buildPeriodUri(int pageNo, int numOfRows, String from, String to) {
		return UriComponentsBuilder.fromUriString(properties.url())
			.path("/period2")
			.queryParam("serviceKey", properties.serviceKey())
			.queryParam("PageNo", pageNo)
			.queryParam("numOfrows", numOfRows)
			.queryParam("from", from)
			.queryParam("to", to)
			.build()
			.toUri();
	}

	private URI buildDetailUri(String seq) {
		return UriComponentsBuilder.fromUriString(properties.url())
			.path("/detail2")
			.queryParam("serviceKey", properties.serviceKey())
			.queryParam("seq", seq)
			.build()
			.toUri();
	}
}
