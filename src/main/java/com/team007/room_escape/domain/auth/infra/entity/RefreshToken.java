package com.team007.room_escape.domain.auth.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "token_hash", nullable = false)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public RefreshToken(Member member, String tokenHash, LocalDateTime expiresAt) {
		this.member = member;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public void rotate(String tokenHash, LocalDateTime expiresAt) {
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public boolean isExpired() {
		return expiresAt.isBefore(LocalDateTime.now());
	}

	public boolean matches(String tokenHash) {
		return this.tokenHash.equals(tokenHash);
	}
}
