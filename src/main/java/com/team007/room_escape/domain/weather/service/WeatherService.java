package com.team007.room_escape.domain.weather.service;

import com.team007.room_escape.domain.festival.infra.entity.FestivalRegion;
import com.team007.room_escape.domain.weather.dto.WeatherCondition;
import com.team007.room_escape.domain.weather.dto.WeatherResponse;
import com.team007.room_escape.domain.weather.infra.client.KmaWeatherApiClient;
import com.team007.room_escape.domain.weather.infra.client.WeatherRegionMapping;
import com.team007.room_escape.domain.weather.infra.client.WeatherRegionPoint;
import com.team007.room_escape.domain.weather.infra.dto.ShortTermForecastItem;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기상청 단기예보 기반 날씨 조회.
 * 미래 날짜의 제공 범위는 고정 일수로 정하지 않고, KMA 응답에 해당 날짜 데이터가 있는지로 자연히 정해진다.
 * 외부 API 실패도 화면이 깨지면 안 되는 부가 기능이라, 예외를 던지지 않고 UNKNOWN으로 감싸서 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 단기예보 발표 시간 (하루 8회 -> 3시간 주기) */
    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    /** 발표 후 실제 조회 가능해지기까지의 지연 */
    private static final Duration PUBLISH_DELAY = Duration.ofMinutes(10);

    private final KmaWeatherApiClient client;

    public WeatherResponse getWeather(FestivalRegion region, LocalDate date) {
        // 과거 날짜만 막는다. 미래 상한은 KMA 응답에 그 날짜 데이터가 있는지로 shortTerm() 안에서 자연히 정해진다.
        if (date.isBefore(LocalDate.now())) {
            return WeatherResponse.unknown(date);
        }
        try {
            return shortTerm(region, date);
        } catch (Exception e) {
            // API 호출·파싱 실패 격리. 날씨는 부가 기능이라 실패해도 UNKNOWN으로 조용히 처리
            log.warn("날씨 조회 실패 (region={}, date={})", region, date, e);
            return WeatherResponse.unknown(date);
        }
    }

    private WeatherResponse shortTerm(FestivalRegion region, LocalDate date) {
        // 지역 → 격자 좌표 변환
        WeatherRegionPoint point = WeatherRegionMapping.resolve(region);
        // 현재 시각 기준 가장 최근 발표 회차 산출
        LocalDateTime base = resolveBase(LocalDateTime.now());

        List<ShortTermForecastItem> items = client.fetchShortTerm(
            point.nx(), point.ny(),
            base.format(DATE_FORMAT), String.format("%02d00", base.getHour())
        );

        String targetDate = date.format(DATE_FORMAT);
        // 하늘상태·강수형태·강수확률 추출
        String sky = pickValue(items, targetDate, "SKY");
        String pty = pickValue(items, targetDate, "PTY");
        if (sky == null && pty == null) {
            return WeatherResponse.unknown(date);
        }
        return new WeatherResponse(toCondition(sky, pty), parseInt(pickValue(items, targetDate, "POP")), date);
    }

    /** 해당 날짜의 하루 대표값 선정. 정오(12시)와 가장 가까운 발표시각의 값 채택 */
    private String pickValue(List<ShortTermForecastItem> items, String targetDate, String category) {
        return items.stream()
            .filter(item -> item.fcstDate().equals(targetDate) && item.category().equals(category))
            .min(Comparator.comparingInt(item -> Math.abs(Integer.parseInt(item.fcstTime().substring(0, 2)) - 12)))
            .map(ShortTermForecastItem::fcstValue)
            .orElse(null);
    }

    /** SKY/PTY 코드 → WeatherCondition 매핑. 강수형태(PTY)가 하늘상태(SKY)보다 우선 판단 */
    private WeatherCondition toCondition(String sky, String pty) {
        if (pty != null) {
            switch (pty) {
                case "1", "4", "5" -> {
                    return WeatherCondition.RAIN; // 비, 소나기, 빗방울
                }
                case "2", "6" -> {
                    return WeatherCondition.RAIN_SNOW; // 비/눈, 빗방울눈날림
                }
                case "3", "7" -> {
                    return WeatherCondition.SNOW; // 눈, 눈날림
                }
                default -> {
                    // "0"(강수 없음)이면 SKY로 판단 계속
                }
            }
        }
        if (sky != null) {
            return switch (sky) {
                case "1" -> WeatherCondition.SUNNY;
                case "3", "4" -> WeatherCondition.CLOUDY;
                default -> WeatherCondition.UNKNOWN;
            };
        }
        return WeatherCondition.UNKNOWN;
    }

    /** 문자열 → 정수 안전 변환. 형식 오류 시 null 반환 */
    private Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 발표 후 10분 지연 반영, 이미 발표 완료된 가장 최근 회차 계산 */
    private LocalDateTime resolveBase(LocalDateTime now) {
        LocalDateTime cutoff = now.minus(PUBLISH_DELAY);
        for (int i = BASE_HOURS.length - 1; i >= 0; i--) {
            LocalDateTime candidate = cutoff.toLocalDate().atTime(BASE_HOURS[i], 0);
            if (!candidate.isAfter(cutoff)) {
                return candidate;
            }
        }
        // 오늘 02시 발표(-10분 여유)보다 이른 새벽이면 전날 23시 발표로 대체
        return cutoff.toLocalDate().minusDays(1).atTime(23, 0);
    }
}
