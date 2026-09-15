package com.team007.room_escape.domain.festival.infra.dto;

import java.util.List;

public record FestivalApiResult(
    int totalCount, //전체 건 수
    List<FestivalApiRow> rows //이전 페이지에서 받은 행사 목록
) {
}
