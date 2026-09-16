package com.team007.room_escape.domain.member.infra.entity;

/**
 * 회원 신뢰 등급 겸 시큐리티 권한.
 * 아래 선언 순서가 곧 권한 계층이며, SecurityConfig.roleHierarchy()와 항상 동일하게 유지해야 한다.
 * ROLE_WARNING(최하) → ROLE_UNVERIFIED → ROLE_NORMAL → ROLE_TRUSTED → ROLE_ADMIN(최상)
 */
public enum MemberRole {

	/** 허위/미개최/중대한 신고 누적. 행사 등록 등 주요 기능이 제한된다 */
	ROLE_WARNING,

	/** 신규 주최자. 가입 직후 기본값 */
	ROLE_UNVERIFIED,

	/** 정상적인 행사 개최 이력 보유 */
	ROLE_NORMAL,

	/** 여러 차례 정상 개최. 민간행사 신청 가능 */
	ROLE_TRUSTED,

	/** 관리자 */
	ROLE_ADMIN;

	/** 이 등급이 target 등급의 권한을 포함하는지. enum 선언 순서에 의존한다. */
	public boolean includes(MemberRole target) {
		return this.ordinal() >= target.ordinal();
	}

	public boolean isAdmin() {
		return this == ROLE_ADMIN;
	}
}
