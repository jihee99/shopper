package com.jihee.shopper.domain.cart.dto;

import com.jihee.shopper.domain.cart.entity.CartItem;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 장바구니 조회 응답 DTO (ADR-04-004: ACTIVE 상품만 포함).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CartResponse {

    private final List<CartItemInfo> items;
    private final Integer totalPrice;

    public static CartResponse of(List<CartItem> cartItems) {
        // ACTIVE 상품만 필터링 (ADR-04-004)
        List<CartItemInfo> items = cartItems.stream()
                .filter(item -> item.getProduct().getStatus() == ProductStatus.ACTIVE)
                .map(CartItemInfo::from)
                .toList();

        int totalPrice = items.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        return new CartResponse(items, totalPrice);
    }

    @Getter
    @RequiredArgsConstructor
    public static class CartItemInfo {
        private final Long cartItemId;
        private final Long productId;
        private final String productName;
        private final Integer price;
        private final Integer quantity;
        private final Integer stock;
        private final String mainImageUrl;

        public static CartItemInfo from(CartItem cartItem) {
            String mainImageUrl = cartItem.getProduct().getImages().stream()
                    .filter(img -> img.isMain())
                    .findFirst()
                    .map(img -> img.getUrl())
                    .orElse(null);

            return new CartItemInfo(
                    cartItem.getId(),
                    cartItem.getProduct().getId(),
                    cartItem.getProduct().getName(),
                    cartItem.getProduct().getPrice(),
                    cartItem.getQuantity(),
                    cartItem.getProduct().getStock(),
                    mainImageUrl
            );
        }
    }
}
