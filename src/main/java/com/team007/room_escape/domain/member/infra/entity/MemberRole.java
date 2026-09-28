package com.team007.room_escape.domain.member.infra.entity;

/**
 * 회원 신뢰 등급 겸 시큐리티 권한.
 * 아래 선언 순서가 곧 권한 계층이며, SecurityConfig.roleHierarchy()와 항상 동일하게 유지해야 한다.
 * ROLE_WARNING(최하) → ROLE_UNVERIFIED → ROLE_RECOGNIZED → ROLE_TRUSTED → ROLE_ADMIN(최상)
 */
public enum MemberRole {

	/** 허위/미개최/중대한 신고 누적. 행사 등록 등 주요 기능이 제한된다 */
	ROLE_WARNING,

	/** 가입 직후 기본 등급. 화면 표시: 탈출 꿈나무 */
	ROLE_UNVERIFIED,

	/** 행사 제보가 받은 좋아요 또는 '정확해요' 중 하나가 10개 이상. 화면 표시: 탈출 메이커 */
	ROLE_RECOGNIZED,

	/** 행사 제보가 받은 좋아요와 '정확해요'가 모두 10개 이상. 화면 표시: 탈출 마스터 */
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
