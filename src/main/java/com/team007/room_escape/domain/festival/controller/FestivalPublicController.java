package com.team007.room_escape.domain.festival.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team007.room_escape.domain.festival.service.FestivalPublicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalPublicController {

    private final FestivalPublicService festivalPublicService;
    
}
