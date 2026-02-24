package com.jihee.shopper.domain.cart.entity;

import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 상품 엔티티 (ADR-04-002 ~ ADR-04-004).
 *
 * <p>중복 상품은 수량 증가로 처리 (ADR-04-002)
 * <p>최대 수량은 재고와 동일 (ADR-04-003)
 * <p>상품 삭제 시 조회에서 제외 (ADR-04-004)
 */
@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    public static CartItem of(Cart cart, Product product, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.cart = cart;
        cartItem.product = product;
        cartItem.quantity = quantity;
        return cartItem;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    /**
     * 수량 증가 (ADR-04-002: 중복 상품 처리).
     */
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    /**
     * 수량 변경.
     */
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}
