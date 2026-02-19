package com.jihee.shopper.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jihee.shopper.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 API 응답의 공통 래퍼 클래스 (ADR-01-005).
 *
 * <p>성공 응답:
 * <pre>{ "success": true, "message": "...", "data": {...} }</pre>
 *
 * <p>실패 응답:
 * <pre>{ "success": false, "code": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다" }</pre>
 *
 * <p>null 필드는 JSON 직렬화에서 제외된다 (@JsonInclude).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;     // 에러 코드 (실패 시에만 존재)
    private final String message;
    private final T data;

    // ── 성공 팩토리 메서드 ──────────────────────────────────────────────────

    /** 데이터와 메시지를 포함한 성공 응답 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, null, message, data);
    }

    /** 데이터만 포함한 성공 응답 (기본 메시지) */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, "요청이 성공했습니다", data);
    }

    /** 메시지만 포함한 성공 응답 (데이터 없음, 예: 삭제 성공) */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    // ── 실패 팩토리 메서드 ──────────────────────────────────────────────────

    /** ErrorCode의 기본 메시지를 사용하는 실패 응답 */
    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.name(), errorCode.getMessage(), null);
    }

    /** 커스텀 메시지를 사용하는 실패 응답 (Validation 에러 메시지 등) */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.name(), message, null);
    }
}
