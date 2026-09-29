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

	/**
	 * 탈퇴하지 않은 회원만 조회한다.
	 * Member는 탈퇴 후에도 작성한 글·댓글이 남아야 해서 엔티티에 @SQLRestriction을 걸 수 없다.
	 * 따라서 "활동 가능한 회원"을 찾을 때는 findById 대신 이 메서드를 쓴다.
	 */
	Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByIdAndDeletedAtIsNull(UUID id);

	Optional<Member> findByEmailAndDeletedAtIsNull(String email);

	/**
	 * 탈퇴 회원까지 포함해 이메일이 이미 쓰이는지 본다.
	 * 탈퇴 직후 같은 이메일로 다시 가입하는 것을 막기 위함이다.
	 * 보관 기간이 지나 파기 배치가 이메일을 익명화하면 다시 가입할 수 있다.
	 *
	 * 주의: 이 검사는 애플리케이션에만 있다. DB의 uk_member_email 은
	 * WHERE deleted_at IS NULL 이라 탈퇴 행의 이메일을 막지 않는다.
	 * 즉 "활성 회원끼리 이메일 중복"은 DB가 보장하지만
	 * "탈퇴 회원 이메일로 재가입"은 여기서만 막는다.
	 */
	boolean existsByEmail(String email);

	boolean existsByEmailAndDeletedAtIsNull(String email);

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	/**
	 * 보관 기간이 지난 탈퇴 회원의 개인정보를 지운다.
	 *
	 * 행을 DELETE 하지 않는 이유: 이 회원이 쓴 후기·댓글이 @ManyToOne 으로 물려 있어
	 * 행을 지우면 그 글들의 연관관계가 깨진다. 개인정보만 비우고 행은 남긴다.
	 *
	 * email 이 NOT NULL 이라 null 대신 더미값을 넣는다. 이 값이 들어가면
	 * 원래 이메일 자리가 비워져 그때부터 재가입이 가능해진다.
	 *
	 * clearAutomatically: 벌크 UPDATE 는 영속성 컨텍스트를 거치지 않아
	 * 같은 트랜잭션에서 다시 조회하면 옛 값을 볼 수 있다.
	 */
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
