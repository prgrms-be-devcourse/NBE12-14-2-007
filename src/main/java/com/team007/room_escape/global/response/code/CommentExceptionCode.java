    package com.team007.room_escape.global.response.code;

    import com.team007.room_escape.global.exception.ExceptionCode;
    import lombok.Getter;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.HttpStatus;

    @Getter
    @RequiredArgsConstructor
    public enum CommentExceptionCode implements ExceptionCode {

        /** 댓글이 존재하지 않을 때*/
        COMMENT_NOT_FOUND("COMMENT000", HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
        /** 로그인한 회원이 댓글의 작성자가 아닐 때*/
        COMMENT_FORBIDDEN("COMMENT001", HttpStatus.FORBIDDEN, "댓글을 수정하거나 삭제할 권한이 없습니다.");

        private final String code;
        private final HttpStatus status;
        private final String message;
    }
