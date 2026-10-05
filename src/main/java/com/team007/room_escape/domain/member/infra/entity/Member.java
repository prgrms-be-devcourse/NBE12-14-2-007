package com.team007.room_escape.domain.member.infra.entity;

import static com.team007.room_escape.global.util.StringUtil.emptyToNull;

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

// 탈퇴 회원의 글·댓글 조회가 깨지지 않게 @SQLRestriction을 걸지 않는다. 활동 회원은 deletedAtIsNull 메서드로 거른다.
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

	/** ROLE_WARNING(제재) 등급인지. 정보 수정 등 일부 기능을 막는 데 쓴다. */
	public boolean isRestricted() {
		return role == MemberRole.ROLE_WARNING;
	}

	/** 마이페이지 수정. null은 건드리지 않고 빈 문자열이면 지운다. nickname은 지울 수 없다. */
	public void updateProfile(String nickname, String phone, String profileImg) {
		if (nickname != null && !nickname.isBlank()) {
			this.nickname = nickname;
		}
		if (phone != null) {
			this.phone = emptyToNull(phone);
		}
		if (profileImg != null) {
			this.profileImg = emptyToNull(profileImg);
		}
	}

	/** @param encodedPassword 반드시 인코딩된 값이어야 한다. 평문을 넣으면 로그인이 깨진다. */
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	/** 활동 기반 신뢰 등급 재계산용. 제재 회원과 관리자는 자동 계산으로 덮어쓰지 않는다. */
	public void applyTrustGrade(MemberRole grade) {
		if (role == MemberRole.ROLE_WARNING || role == MemberRole.ROLE_ADMIN) {
			return;
		}
		this.role = grade;
	}

	/** 탈퇴 회원 대신 보여줄 이름. 파기 배치와 응답 DTO가 같은 값을 쓰도록 여기 둔다. */
	public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

	/** 관리자가 직접 권한을 바꿀 때만 사용한다. */
	// TODO 변경 사유를 이력 테이블에 남길 것 (행사·후기 숨김 사유와 함께 설계)
	public void changeRole(MemberRole role) {
		this.role = role;
	}
}
