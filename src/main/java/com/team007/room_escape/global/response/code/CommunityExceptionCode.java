package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommunityExceptionCode implements ExceptionCode {
	POST_NOT_FOUND("COMMUNITY000", HttpStatus.NOT_FOUND, "존재하지 않는 커뮤니티 글입니다."),
	POST_FORBIDDEN("COMMUNITY001", HttpStatus.FORBIDDEN, "해당 글을 수정하거나 삭제할 권한이 없습니다."),
	COMMENT_NOT_FOUND("COMMUNITY002", HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
	COMMENT_FORBIDDEN("COMMUNITY003", HttpStatus.FORBIDDEN, "해당 댓글을 수정하거나 삭제할 권한이 없습니다."),
	POST_CREATE_TOO_SOON("COMMUNITY004", HttpStatus.TOO_MANY_REQUESTS, "게시글은 30초에 한 번만 등록할 수 있습니다."),
	COMMENT_CREATE_TOO_SOON("COMMUNITY005", HttpStatus.TOO_MANY_REQUESTS, "댓글은 5초에 한 번만 등록할 수 있습니다."),
	POST_DUPLICATE("COMMUNITY006", HttpStatus.CONFLICT, "같은 내용의 게시글이 이미 등록되어 있습니다."),
	COMMENT_DUPLICATE("COMMUNITY007", HttpStatus.CONFLICT, "같은 내용의 댓글이 이미 등록되어 있습니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
}
