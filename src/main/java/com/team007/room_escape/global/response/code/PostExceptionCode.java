package com.team007.room_escape.global.response.code;

import com.team007.room_escape.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostExceptionCode implements ExceptionCode {

    POST_NOT_FOUND(
            "POST-001",
            HttpStatus.NOT_FOUND,
            "존재하지 않는 후기입니다."
    );

    private final String code;
    private final HttpStatus status;
    private final String message;
}
