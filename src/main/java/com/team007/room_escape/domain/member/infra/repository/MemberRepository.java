package com.team007.room_escape.domain.member.infra.repository;

import com.team007.room_escape.domain.member.infra.entity.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
