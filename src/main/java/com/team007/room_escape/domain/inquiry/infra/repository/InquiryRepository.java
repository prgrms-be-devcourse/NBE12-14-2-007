package com.team007.room_escape.domain.inquiry.infra.repository;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

	/**
	 * 내가 쓴 문의 목록. 최신순.
	 * 삭제된 문의는 엔티티의 @SQLRestriction 이 걸러주므로 조건을 따로 적지 않는다.
	 */
	List<Inquiry> findAllByMember_IdOrderByCreatedAtDesc(UUID memberId);
}
