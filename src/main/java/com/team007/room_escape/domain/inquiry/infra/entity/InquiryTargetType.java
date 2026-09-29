package com.team007.room_escape.domain.inquiry.infra.entity;

/**
 * 신고·제보 대상 종류.
 * 일반 문의는 대상이 없으므로 Inquiry의 targetType이 null이다.
 */
public enum InquiryTargetType {
	FESTIVAL,
	POST,
	COMMENT,
	MEMBER,
	COMMUNITY_POST,
	COMMUNITY_COMMENT
}
