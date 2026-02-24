package com.jihee.shopper.domain.order.entity;

/**
 * 주문 상태 (ADR-04-009).
 *
 * <p>PENDING: 주문 생성 (결제 대기)
 * <p>PAID: 결제 완료
 * <p>CANCELLED: 주문 취소
 */
public enum OrderStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    CANCELLED   // 주문 취소
}
