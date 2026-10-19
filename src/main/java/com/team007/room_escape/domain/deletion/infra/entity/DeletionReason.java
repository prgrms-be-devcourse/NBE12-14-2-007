package com.team007.room_escape.domain.deletion.infra.entity;

public enum DeletionReason {
	AUTHOR_REQUEST("작성자 직접 삭제"),
	DUPLICATE("중복 등록"),
	INCORRECT_INFORMATION("잘못된 정보"),
	SPAM("도배·스팸"),
	ABUSIVE_CONTENT("욕설·비방"),
	PRIVACY_EXPOSURE("개인정보 노출"),
	OTHER("기타"),
	ADMIN_RESTORE("관리자 복구");

	private final String description;

	DeletionReason(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
