package com.team007.room_escape.domain.inquiry.infra.repository;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
}
