package com.jihee.shopper.domain.order.entity;

import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품 엔티티 (ADR-04-007).
 *
 * <p>주문 시점 가격과 상품명을 스냅샷으로 저장한다.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    /** 주문 시점 가격 스냅샷 (ADR-04-007) */
    @Column(nullable = false)
    private Integer price;

    /** 주문 시점 상품명 스냅샷 (ADR-04-007) */
    @Column(nullable = false)
    private String productName;

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    /**
     * OrderItem 생성 (가격, 상품명 스냅샷).
     */
    public static OrderItem of(Order order, Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.product = product;
        item.quantity = quantity;
        item.price = product.getPrice();        // 스냅샷
        item.productName = product.getName();   // 스냅샷
        return item;
    }
}
