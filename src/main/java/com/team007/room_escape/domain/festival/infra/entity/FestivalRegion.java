package com.team007.room_escape.domain.festival.infra.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FestivalRegion {

	// 광역 지역
	SEOUL("서울특별시"),
	BUSAN("부산광역시"),
	DAEGU("대구광역시"),
	INCHEON("인천광역시"),
	GWANGJU("광주광역시"),
	DAEJEON("대전광역시"),
	ULSAN("울산광역시"),
	SEJONG("세종특별자치시"),
	GANGWON("강원특별자치도"),
	CHUNGBUK("충청북도"),
	CHUNGNAM("충청남도"),
	JEONBUK("전북특별자치도"),
	JEONNAM("전라남도"),
	GYEONGBUK("경상북도"),
	GYEONGNAM("경상남도"),
	JEJU("제주특별자치도"),
	GYEONGGI("경기도"),

	// TODO: 사용자 제보 데이터가 충분히 쌓이면 시·군·구를 별도 필드로 분리하고 검색 필터 도입 검토
	// 경기도 31개 시·군
	GYEONGGI_SUWON("경기도 수원시"),
	GYEONGGI_GOYANG("경기도 고양시"),
	GYEONGGI_YONGIN("경기도 용인시"),
	GYEONGGI_SEONGNAM("경기도 성남시"),
	GYEONGGI_BUCHEON("경기도 부천시"),
	GYEONGGI_HWASEONG("경기도 화성시"),
	GYEONGGI_ANSAN("경기도 안산시"),
	GYEONGGI_NAMYANGJU("경기도 남양주시"),
	GYEONGGI_ANYANG("경기도 안양시"),
	GYEONGGI_PYEONGTAEK("경기도 평택시"),
	GYEONGGI_SIHEUNG("경기도 시흥시"),
	GYEONGGI_PAJU("경기도 파주시"),
	GYEONGGI_UIJEONGBU("경기도 의정부시"),
	GYEONGGI_GIMPO("경기도 김포시"),
	GYEONGGI_GWANGJU("경기도 광주시"),
	GYEONGGI_GWANGMYEONG("경기도 광명시"),
	GYEONGGI_GUNPO("경기도 군포시"),
	GYEONGGI_HANAM("경기도 하남시"),
	GYEONGGI_OSAN("경기도 오산시"),
	GYEONGGI_YANGJU("경기도 양주시"),
	GYEONGGI_ICHEON("경기도 이천시"),
	GYEONGGI_GURI("경기도 구리시"),
	GYEONGGI_ANSEONG("경기도 안성시"),
	GYEONGGI_POCHEON("경기도 포천시"),
	GYEONGGI_UIWANG("경기도 의왕시"),
	GYEONGGI_YANGPYEONG("경기도 양평군"),
	GYEONGGI_YEOJU("경기도 여주시"),
	GYEONGGI_DONGDUCHEON("경기도 동두천시"),
	GYEONGGI_GWACHEON("경기도 과천시"),
	GYEONGGI_GAPYEONG("경기도 가평군"),
	GYEONGGI_YEONCHEON("경기도 연천군");

	private final String displayName;

	// 긴 접미사부터 검사해야 "특별자치시"가 "시"로 먼저 잘리는 걸 막는다.
	private static final List<String> ADMIN_SUFFIXES = List.of(
		"특별자치시", "특별자치도", "광역시", "특별시", "자치도", "도", "시", "군", "구"
	);

	// 경기도 하위 시·군(공백 포함 displayName)은 제외하고, 광역 단위만 미리 인덱싱해둔다.
	private static final Map<String, FestivalRegion> BY_NORMALIZED_AREA = Arrays.stream(values())
		.filter(region -> !region.displayName.contains(" "))
		.collect(Collectors.toMap(region -> normalizeUnit(region.displayName), Function.identity()));

	/**
	 * 새 API의 area를 FestivalRegion(광역 단위)으로 바꾼다.
	 * "강원특별자치도"처럼 접미사가 풀로 오든 "강원"처럼 줄여서 오든 둘 다 받아들인다.
	 * 시군구는 여기서 다루지 않고 Festival.regionDetail에 별도로 저장한다.
	 */
	public static FestivalRegion fromDisplayName(String area) {
		if (area == null || area.isBlank()) {
			return null;
		}
		return BY_NORMALIZED_AREA.get(normalizeUnit(area));
	}

	private static String normalizeUnit(String value) {
		String trimmed = value.trim();
		for (String suffix : ADMIN_SUFFIXES) {
			if (trimmed.length() > suffix.length() && trimmed.endsWith(suffix)) {
				return trimmed.substring(0, trimmed.length() - suffix.length());
			}
		}
		return trimmed;
	}
}
