package com.jihee.shopper.global.exception;

import lombok.Getter;

/**
 * 애플리케이션 비즈니스 예외의 기반 클래스.
 *
 * <p>서비스 레이어에서 비즈니스 규칙 위반 시 throw한다.
 * GlobalExceptionHandler가 이 예외를 catch하여 적절한 HTTP 응답을 생성한다.
 *
 * <pre>
 * // 사용 예시
 * throw new CustomException(ErrorCode.USER_NOT_FOUND);
 * throw new CustomException(ErrorCode.INVALID_INPUT, "이메일 형식이 올바르지 않습니다");
 * </pre>
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    /** ErrorCode의 기본 메시지를 사용하는 예외 */
    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 커스텀 메시지를 사용하는 예외 */
    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
