package com.jihee.shopper.domain.product.entity;

/**
 * 상품 상태 (ADR-03-004).
 *
 * <p>ACTIVE: 판매 중
 * <p>INACTIVE: 판매 중지 (소프트 삭제 포함)
 */
public enum ProductStatus {
    ACTIVE,    // 판매 중
    INACTIVE   // 판매 중지 (삭제 포함)
}
