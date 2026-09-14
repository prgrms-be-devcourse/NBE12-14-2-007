package com.team007.room_escape.domain.festival.service;

import com.team007.room_escape.domain.festival.infra.repository.FestivalRepository;
import com.team007.room_escape.domain.festival.infra.repository.PublicFestivalSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FestivalService {

	private final FestivalRepository festivalRepository;
	private final PublicFestivalSourceRepository publicFestivalSourceRepository;
}
