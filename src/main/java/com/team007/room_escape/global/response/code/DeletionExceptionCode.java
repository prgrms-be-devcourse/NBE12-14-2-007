package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 삭제·복구 이력(deletion) 공통 에러 코드.
 * 행사·후기·댓글 등 대상 종류와 무관하게 삭제 절차 자체에서 생기는 실패에 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum DeletionExceptionCode implements ExceptionCode {

	/** 대상 ID가 비었거나 대상 종류가 요구하는 형식(UUID 또는 양수)이 아닐 때 */
	INVALID_TARGET_ID("DELETION000", HttpStatus.BAD_REQUEST, "삭제 대상 ID가 올바르지 않습니다."),

	/** 대상 종류가 비어 있을 때 */
	INVALID_TARGET_TYPE("DELETION001", HttpStatus.BAD_REQUEST, "삭제 대상 종류가 올바르지 않습니다."),

	/**
	 * 대상 종류에 맞는 DeletionHandler 가 등록되어 있지 않을 때.
	 * 요청이 아니라 서버 구성의 문제이므로 500으로 알린다.
	 * (DeletionTargetType 에 값을 추가하고 핸들러를 만들지 않으면 발생한다.)
	 */
	HANDLER_NOT_REGISTERED("DELETION002", HttpStatus.INTERNAL_SERVER_ERROR,
		"삭제를 처리할 수 없는 대상 종류입니다."),

	/** 이력에 필요한 값이 비었거나 사유 상세가 500자를 넘을 때 */
	INVALID_DELETION_REQUEST("DELETION003", HttpStatus.BAD_REQUEST, "삭제 요청 값이 올바르지 않습니다."),

	/** 사유를 '기타'로 고르면 상세 사유를 반드시 받는다 */
	REASON_DETAIL_REQUIRED("DELETION004", HttpStatus.BAD_REQUEST, "기타 사유를 선택한 경우 상세 사유를 입력해야 합니다."),

	/** 조치·주체·사유 조합이 맞지 않을 때 (예: 작성자 삭제에 관리자 사유를 넣은 경우) */
	INVALID_ACTOR_FOR_ACTION("DELETION005", HttpStatus.BAD_REQUEST, "삭제 주체와 사유가 맞지 않습니다."),

	/** 삭제 이력이 없어 관리자 삭제였는지 확인할 수 없을 때. 추측해서 복구하지 않는다. */
	RESTORE_HISTORY_NOT_FOUND("DELETION006", HttpStatus.CONFLICT, "삭제 이력이 없어 복구할 수 없습니다."),

	/** 마지막 조치가 관리자 삭제일 때만 복구한다. 작성자가 스스로 지운 것은 되돌리지 않는다. */
	RESTORE_NOT_ALLOWED("DELETION007", HttpStatus.FORBIDDEN, "관리자가 삭제한 대상만 복구할 수 있습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
