package com.team007.room_escape.domain.manager.infra.entity;

import com.team007.room_escape.domain.member.infra.entity.Member;
import com.team007.room_escape.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "manager")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manager extends BaseTimeEntity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	private String organization;

	@Column(name = "personal_phone")
	private String personalPhone;

	@Column(name = "company_phone")
	private String companyPhone;

	@Column(nullable = false, columnDefinition = "text")
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ManagerStatus status;

	@Column(name = "revoke_reason", columnDefinition = "text")
	private String revokeReason;

	public static Manager apply(
		Member member,
		String organization,
		String personalPhone,
		String companyPhone,
		String reason
	) {
		Manager manager = new Manager();
		manager.member = member;
		manager.organization = organization;
		manager.personalPhone = personalPhone;
		manager.companyPhone = companyPhone;
		manager.reason = reason;
		manager.status = ManagerStatus.APPLYING;
		return manager;
	}

	public void reapply(String organization, String personalPhone, String companyPhone, String reason) {
		this.organization = organization;
		this.personalPhone = personalPhone;
		this.companyPhone = companyPhone;
		this.reason = reason;
		this.status = ManagerStatus.APPLYING;
		this.revokeReason = null;
	}

	public boolean isApplying() {
		return status == ManagerStatus.APPLYING;
	}

	public boolean isCompleted() {
		return status == ManagerStatus.COMPLETED;
	}

	public void complete() {
		this.status = ManagerStatus.COMPLETED;
		this.revokeReason = null;
	}

	public void revoke(String revokeReason) {
		this.status = ManagerStatus.REVOKED;
		this.revokeReason = revokeReason;
	}
}
