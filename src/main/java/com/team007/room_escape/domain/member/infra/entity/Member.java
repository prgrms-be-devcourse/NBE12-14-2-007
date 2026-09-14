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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public static Member signUp(String email, String encodedPassword, String nickname, String phone) {
		Member member = new Member();
		member.email = email;
		member.password = encodedPassword;
		member.role = MemberRole.ROLE_USER;
		member.nickname = nickname;
		member.phone = phone;
		return member;
	}

	public String authority() {
		return role.name();
	}
}
