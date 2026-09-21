package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.dto.FestivalBrowseResponse;
import com.team007.room_escape.domain.festival.infra.entity.FestivalStatus;
import com.team007.room_escape.domain.festival.infra.entity.ProviderType;
import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.global.exception.BusinessException;
import com.team007.room_escape.global.response.code.FestivalExceptionCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalBrowseService {
    private final FestivalRepository festivalRepository;

    public Page<FestivalBrowseResponse> submissions(String query, FestivalStatus status, Pageable page) {
        Boolean closed = status == null ? null : status == FestivalStatus.CLOSED;
        String keyword = query.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return festivalRepository.findSharedSubmissions(
                ProviderType.MEMBER, "%" + keyword + "%", closed, LocalDateTime.now(), page)
                .map(FestivalBrowseResponse::from);
    }

    public FestivalBrowseResponse detail(Long festivalId) {
        return festivalRepository.findSharedFestival(festivalId, ProviderType.PUBLIC)
                .map(FestivalBrowseResponse::from)
                .orElseThrow(() -> new BusinessException(FestivalExceptionCode.FESTIVAL_NOT_FOUND));
    }
}
