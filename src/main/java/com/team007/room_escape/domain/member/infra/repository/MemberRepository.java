package com.team007.room_escape.domain.member.infra.repository;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.domain.member.infra.entity.MemberRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	/**
	 * 탈퇴하지 않은 회원만 조회한다.
	 * Member는 탈퇴 후에도 작성한 글·댓글이 남아야 해서 엔티티에 @SQLRestriction을 걸 수 없다.
	 * 따라서 "활동 가능한 회원"을 찾을 때는 findById 대신 이 메서드를 쓴다.
	 */
	Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

	Optional<Member> findByEmailAndDeletedAtIsNull(String email);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	/**
	 * 관리자용 회원 검색. 모든 조건은 선택이며, 비우면 그 조건을 걸지 않는다.
	 * includeDeleted 가 true면 탈퇴 회원까지 함께 조회한다.
	 *
	 * keyword 는 null 대신 빈 문자열을 받는다. null을 LIKE 에 넘기면 Postgres 가
	 * 파라미터 타입을 추론하지 못해 'operator does not exist: text ~~ bytea' 로 실패한다.
	 * 빈 문자열이면 LIKE '%%' 가 되어 모든 행이 걸리므로 조건을 걸지 않은 것과 같다.
	 */
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
