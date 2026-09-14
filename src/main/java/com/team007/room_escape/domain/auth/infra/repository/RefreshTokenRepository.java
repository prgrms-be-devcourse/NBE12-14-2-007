package com.team007.room_escape.domain.auth.infra.repository;

import com.team007.room_escape.domain.auth.infra.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByMember_Id(UUID memberId);

	void deleteByMember_Id(UUID memberId);

	void deleteByExpiresAtBefore(LocalDateTime time);
}
