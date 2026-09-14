package com.team007.room_escape.domain.inquiry.service;

import com.team007.room_escape.domain.inquiry.infra.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InquiryService {

	private final InquiryRepository inquiryRepository;
}
