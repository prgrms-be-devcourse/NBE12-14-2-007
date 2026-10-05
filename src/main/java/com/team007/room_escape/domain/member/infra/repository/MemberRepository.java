package com.team007.room_escape.domain.member.infra.repository;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	/** 탈퇴하지 않은 회원 조회. Member엔 @SQLRestriction이 없어서 findById 대신 이걸 쓴다. */
	Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByIdAndDeletedAtIsNull(UUID id);

	Optional<Member> findByEmailAndDeletedAtIsNull(String email);

	/** 탈퇴 회원 포함 이메일 중복 확인. 탈퇴 직후 재가입은 DB 인덱스가 아니라 여기서만 막는다. */
	boolean existsByEmail(String email);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	/** 보관 기간이 지난 탈퇴 회원의 개인정보를 비운다. 글이 참조하므로 행은 남긴다. */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE Member m
		SET m.email = CONCAT('purged-', m.id, '@removed.local'),
		    m.nickname = '탈퇴한 사용자',
		    m.phone = null,
		    m.profileImg = null
		WHERE m.deletedAt IS NOT NULL
		  AND m.deletedAt < :threshold
		  AND m.email NOT LIKE 'purged-%'
		""")
	int purgeWithdrawnBefore(@Param("threshold") LocalDateTime threshold);

	/** 관리자 회원 검색. keyword는 null 대신 빈 문자열을 받는다. */
	@Query(value = """
		SELECT m
		FROM Member m
		WHERE (:includeDeleted = true OR m.deletedAt IS NULL)
		  AND (LOWER(m.nickname) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(m.email) LIKE CONCAT('%', :keyword, '%'))
		  AND (:role IS NULL OR m.role = :role)
		""",
		countQuery = """
		SELECT COUNT(m)
		FROM Member m
		WHERE (:includeDeleted = true OR m.deletedAt IS NULL)
		  AND (LOWER(m.nickname) LIKE CONCAT('%', :keyword, '%')
		       OR LOWER(m.email) LIKE CONCAT('%', :keyword, '%'))
		  AND (:role IS NULL OR m.role = :role)
		""")
	Page<Member> search(
		@Param("keyword") String keyword,
		@Param("role") MemberRole role,
		@Param("includeDeleted") boolean includeDeleted,
		Pageable pageable
	);
}
