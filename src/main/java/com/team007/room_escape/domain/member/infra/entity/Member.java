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

// Post, Comment, Like 가 이 엔티티를 @ManyToOne 으로 참조한다.
// @SQLRestriction 을 걸면 탈퇴한 회원이 쓴 글·댓글을 조회할 때 예외가 나므로 걸지 않는다.
// 로그인/가입 경로는 MemberRepository 의 deletedAtIsNull 메서드로 거른다.
//TODO : 탈퇴시 "탈퇴한 사용자입니다" 로 응답에 표시하기
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

	/**
	 * 마이페이지 정보 수정.
	 * PATCH 의미에 맞춰 null인 값은 건드리지 않고, 빈 문자열이면 값을 지운다(null 저장).
	 * nickname은 필수 컬럼이라 빈 문자열을 허용하지 않는다.
	 *
	 * @param profileImg R2 저장 key. 공개 URL이 아니다
	 */
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

	/** 신뢰 등급 재계산 배치용. 관리자 권한은 자동 계산으로 덮어쓰지 않는다. */
	public void applyTrustGrade(MemberRole grade) {
		if (role == MemberRole.ROLE_ADMIN || grade == MemberRole.ROLE_ADMIN) {
			return;
		}
		this.role = grade;
	}

	/** 관리자가 직접 권한을 바꿀 때만 사용한다. */
	// TODO role 변경 사유 추가 필요 (테이블 나눠야 할 듯)
	//      어드민 화면은 이미 사유를 필수로 받고 있는데 저장할 곳이 없어 버려지고 있다.
	//      누가·언제·누구를·왜 바꿨는지가 남아야 하므로 member 컬럼이 아니라
	//      이력 테이블로 빼야 한다. 행사·후기 숨김 처리도 같은 사유를 받으므로
	//      회원 전용으로 만들지 말고 함께 설계할 것.
	public void changeRole(MemberRole role) {
		this.role = role;
	}
}
