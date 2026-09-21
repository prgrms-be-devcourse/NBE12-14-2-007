package com.team007.room_escape.domain.inquiry.infra.entity;

/**
 * 문의 종류.
 *
 * 값 이름을 INQUIRY가 아니라 QUESTION으로 둔 이유는,
 * Inquiry 엔티티의 category가 INQUIRY가 되면 읽기 어색하기 때문이다.
 */
public enum InquiryCategory {

	/** 서비스 이용 관련 일반 문의 */
	QUESTION,

	/** 부적절한 행사·후기·사용자 신고 */
	REPORT
}
