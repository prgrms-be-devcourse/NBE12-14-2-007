package com.team007.room_escape.domain.festival.service;

import org.springframework.stereotype.Service;

import com.team007.room_escape.domain.festival.infra.repository.FestivalPublicRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FestivalPublicService {

    private final FestivalPublicRepository festivalPublicRepository;
    
}
