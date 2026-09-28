package com.team007.room_escape.domain.inquiry.infra.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team007.room_escape.domain.inquiry.infra.entity.Inquiry;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryCategory;
import com.team007.room_escape.domain.inquiry.infra.entity.InquiryStatus;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

	/**
	 * 내가 쓴 문의 목록. 최신순.
	 * 엔티티에 @SQLRestriction 이 없으므로 삭제 제외 조건을 직접 적는다.
	 */
	List<Inquiry> findAllByMember_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID memberId);

	/** 삭제되지 않은 문의 단건. */
	Optional<Inquiry> findByIdAndDeletedAtIsNull(UUID id);

	/**
	 * 관리자용 문의 단건. 작성자까지 함께 가져온다.
	 * 삭제 여부를 가리지 않는다. 관리자는 삭제된 문의도 열어볼 수 있어야 한다.
	 */
	@Query("""
		SELECT i
		FROM Inquiry i
		LEFT JOIN FETCH i.member
		WHERE i.id = :id
		""")
	Optional<Inquiry> findDetailById(@Param("id") UUID id);

	/**
	 * 관리자용 문의 검색. 모든 조건은 선택이며, 비우면 그 조건을 걸지 않는다.
	 * includeDeleted 가 true면 삭제된 문의까지 함께 조회한다.
	 *
	 * title 은 null 대신 빈 문자열을 받는다. null을 LIKE 에 넘기면 Postgres 가
	 * 파라미터 타입을 추론하지 못해 'operator does not exist: text ~~ bytea' 로 실패한다.
	 * 빈 문자열이면 LIKE '%%' 가 되어 모든 행이 걸리므로 조건을 걸지 않은 것과 같다.
	 */
	@Query(value = """
		SELECT i
		FROM Inquiry i
		LEFT JOIN FETCH i.member
		WHERE (:includeDeleted = true OR i.deletedAt IS NULL)
		  AND LOWER(i.title) LIKE CONCAT('%', :title, '%')
		  AND (:status IS NULL OR i.status = :status)
		  AND (:category IS NULL OR i.category = :category)
		""",
		countQuery = """
		SELECT COUNT(i)
		FROM Inquiry i
		WHERE (:includeDeleted = true OR i.deletedAt IS NULL)
		  AND LOWER(i.title) LIKE CONCAT('%', :title, '%')
		  AND (:status IS NULL OR i.status = :status)
		  AND (:category IS NULL OR i.category = :category)
		""")
	Page<Inquiry> search(
		@Param("title") String title,
		@Param("status") InquiryStatus status,
		@Param("category") InquiryCategory category,
		@Param("includeDeleted") boolean includeDeleted,
		Pageable pageable
	);
}
