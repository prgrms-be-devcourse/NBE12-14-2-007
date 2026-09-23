package com.team007.room_escape.domain.weather.infra.client;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import java.util.EnumMap;
import java.util.Map;

/**
 * 17개 시·도를 대표 도시의 기상청 단기예보 격자 좌표로 매핑한다. 격자값은 각 시·도 도청 소재지급
 * 대표 도시 좌표를 기상청 격자 변환 공식으로 계산해, 공개적으로 알려진 값과 대조 검증했다.
 * 날씨는 시·도 단위로만 제공하며, 경기도 시·군(GYEONGGI_* )은 지원하지 않는다.
 */
public final class WeatherRegionMapping {

    /** EnumMap을 쓴 이유 => key가 FestivalRegion이라는 고정된 enum이기 때문 일반 HashMap보다 내부적으로 배열 기반이라 더 빠름**/
    private static final Map<FestivalRegion, WeatherRegionPoint> POINTS = new EnumMap<>(FestivalRegion.class);

    static {
        POINTS.put(FestivalRegion.SEOUL, new WeatherRegionPoint(60, 127));
        POINTS.put(FestivalRegion.INCHEON, new WeatherRegionPoint(55, 124));
        POINTS.put(FestivalRegion.GYEONGGI, new WeatherRegionPoint(61, 120));
        POINTS.put(FestivalRegion.GANGWON, new WeatherRegionPoint(73, 134));
        POINTS.put(FestivalRegion.CHUNGBUK, new WeatherRegionPoint(69, 107));
        POINTS.put(FestivalRegion.CHUNGNAM, new WeatherRegionPoint(62, 110));
        POINTS.put(FestivalRegion.DAEJEON, new WeatherRegionPoint(67, 100));
        POINTS.put(FestivalRegion.SEJONG, new WeatherRegionPoint(66, 103));
        POINTS.put(FestivalRegion.JEONBUK, new WeatherRegionPoint(63, 89));
        POINTS.put(FestivalRegion.JEONNAM, new WeatherRegionPoint(50, 67));
        POINTS.put(FestivalRegion.GWANGJU, new WeatherRegionPoint(58, 74));
        POINTS.put(FestivalRegion.GYEONGBUK, new WeatherRegionPoint(102, 94));
        POINTS.put(FestivalRegion.DAEGU, new WeatherRegionPoint(89, 91));
        POINTS.put(FestivalRegion.GYEONGNAM, new WeatherRegionPoint(91, 77));
        POINTS.put(FestivalRegion.BUSAN, new WeatherRegionPoint(98, 76));
        POINTS.put(FestivalRegion.ULSAN, new WeatherRegionPoint(102, 84));
        POINTS.put(FestivalRegion.JEJU, new WeatherRegionPoint(53, 38));
    }

    private WeatherRegionMapping() {
    }

    public static WeatherRegionPoint resolve(FestivalRegion region) {
        WeatherRegionPoint point = POINTS.get(region);
        if (point == null) {
            throw new IllegalArgumentException("날씨를 지원하지 않는 지역입니다: " + region);
        }
        return point;
    }
}
