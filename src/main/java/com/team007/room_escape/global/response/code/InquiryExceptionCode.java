package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquiryExceptionCode implements ExceptionCode {

	INQUIRY_NOT_FOUND("INQUIRY000", HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
	INQUIRY_UPDATE_FORBIDDEN("INQUIRY001", HttpStatus.FORBIDDEN, "해당 문의를 수정할 권한이 없습니다."),
	INQUIRY_DELETE_FORBIDDEN("INQUIRY002", HttpStatus.FORBIDDEN, "해당 문의를 삭제할 권한이 없습니다."),
	/** 답변이 달린 뒤에 내용을 바꾸면 답변과 질문이 맞지 않게 된다 */
	INQUIRY_ALREADY_ANSWERED("INQUIRY003", HttpStatus.CONFLICT, "답변이 완료된 문의는 수정할 수 없습니다."),
	/** 작성자가 지운 문의에 답변해도 작성자에게 보이지 않는다 */
	INQUIRY_ALREADY_DELETED("INQUIRY004", HttpStatus.CONFLICT, "삭제된 문의에는 답변할 수 없습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
