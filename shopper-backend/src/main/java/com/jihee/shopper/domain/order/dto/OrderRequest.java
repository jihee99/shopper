package com.jihee.shopper.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO (ADR-04-012: 부분 주문 지원).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "배송지는 필수입니다")
    private Long addressId;

    @NotEmpty(message = "주문할 상품을 선택해주세요")
    private List<Long> cartItemIds;  // 선택된 CartItem ID 목록
}
