package com.team007.room_escape.domain.member.infra.entity;

import com.team007.room_escape.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "member")
@Getter
@Builder 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends SoftDeletableEntity {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private MemberRole role;

	@Column(nullable = false)
	private String nickname;

	@Column(name = "profile_img", length = 2048)
	private String profileImg;

	private String phone;

	public String authority() {
		return role.name();
	}

	public boolean hasPrivilegeOf(MemberRole required) {
		return role.includes(required);
	}

	/** 신뢰 등급 재계산 배치용. 관리자 권한은 자동 계산으로 덮어쓰지 않는다. */
	public void applyTrustGrade(MemberRole grade) {
		if (role == MemberRole.ROLE_ADMIN || grade == MemberRole.ROLE_ADMIN) {
			return;
		}
		this.role = grade;
	}

	/** 관리자가 직접 권한을 바꿀 때만 사용한다. */
	public void changeRole(MemberRole role) {
		this.role = role;
	}
}
