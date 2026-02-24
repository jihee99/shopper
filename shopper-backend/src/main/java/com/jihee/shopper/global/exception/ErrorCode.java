package com.jihee.shopper.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 전역 에러 코드 정의 (ADR-01-006).
 *
 * <p>단일 enum에 도메인별 그룹으로 관리한다.
 * 각 ErrorCode는 HTTP 상태 코드와 메시지를 함께 보유하며,
 * GlobalExceptionHandler가 이를 참조해 응답을 생성한다.
 *
 * <p>새 에러 코드 추가 시 해당 도메인 그룹 아래에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────
    INVALID_INPUT(400, "잘못된 입력값입니다"),
    UNAUTHORIZED(401, "인증이 필요합니다"),
    FORBIDDEN(403, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다"),

    // ── 인증 / 회원 ──────────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다"),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    INVALID_PASSWORD(401, "비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다"),
    TOKEN_NOT_FOUND(401, "토큰을 찾을 수 없습니다"),
    ADDRESS_NOT_FOUND(404, "배송지를 찾을 수 없습니다"),
    ADDRESS_IN_USE(400, "주문에서 사용 중인 배송지는 삭제할 수 없습니다"),

    // ── 상품 ─────────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND(404, "상품을 찾을 수 없습니다"),
    CATEGORY_NOT_FOUND(404, "카테고리를 찾을 수 없습니다"),
    CATEGORY_DEPTH_EXCEEDED(400, "카테고리는 최대 3단계까지만 생성할 수 있습니다"),
    CATEGORY_HAS_PRODUCTS(400, "하위 상품이 존재하여 삭제할 수 없습니다"),
    OUT_OF_STOCK(409, "재고가 부족합니다"),

    // ── 장바구니 ──────────────────────────────────────────────────────────
    CART_ITEM_NOT_FOUND(404, "장바구니 상품을 찾을 수 없습니다"),
    CART_ITEM_QUANTITY_INVALID(400, "수량은 1 이상이어야 합니다"),

    // ── 주문 ─────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다"),
    ORDER_CANCEL_NOT_ALLOWED(400, "취소할 수 없는 주문 상태입니다"),
    ORDER_ALREADY_PAID(400, "이미 결제 완료된 주문입니다"),

    // ── 결제 ─────────────────────────────────────────────────────────────
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않습니다"),
    PAYMENT_ALREADY_COMPLETED(409, "이미 완료된 결제입니다"),
    PAYMENT_NOT_FOUND(404, "결제 정보를 찾을 수 없습니다"),
    PAYMENT_CONFIRM_FAILED(500, "결제 승인에 실패했습니다"),

    // ── 파일 ─────────────────────────────────────────────────────────────
    FILE_UPLOAD_FAILED(500, "파일 업로드에 실패했습니다"),
    INVALID_FILE_TYPE(400, "지원하지 않는 파일 형식입니다"),
    FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB를 초과할 수 없습니다"),
    IMAGE_LIMIT_EXCEEDED(400, "상품당 이미지는 최대 10개까지 업로드할 수 있습니다"),
    PRODUCT_IMAGE_NOT_FOUND(404, "상품 이미지를 찾을 수 없습니다");

    private final int httpStatus;
    private final String message;
}
