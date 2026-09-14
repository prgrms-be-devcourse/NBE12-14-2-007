package com.team007.room_escape.domain.festival.controller;

import com.team007.room_escape.domain.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalController {

	private final FestivalService festivalService;
}
