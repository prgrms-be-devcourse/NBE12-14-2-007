package com.team007.room_escape.domain.inquiry.controller;

import com.team007.room_escape.domain.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

	private final InquiryService inquiryService;
}
