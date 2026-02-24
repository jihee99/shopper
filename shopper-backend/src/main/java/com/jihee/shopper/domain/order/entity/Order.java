package com.jihee.shopper.domain.order.entity;

import com.jihee.shopper.domain.user.entity.Address;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티 (ADR-04-006 ~ ADR-04-010).
 *
 * <p>배송지는 Address FK 참조 (ADR-04-006)
 * <p>총액은 저장 필드 (ADR-04-008)
 * <p>초기 상태는 PENDING (ADR-04-009)
 * <p>주문 생성 시 재고 차감 (ADR-04-010)
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 주문 번호 (ADR-04-013)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;  // 배송지 FK (ADR-04-006)

    @Column(nullable = false)
    private Integer totalPrice;  // 주문 총액 (ADR-04-008)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    /**
     * 주문 생성 (ADR-04-009: 초기 상태 PENDING).
     */
    public static Order create(User user, Address address, int totalPrice) {
        Order order = new Order();
        order.user = user;
        order.address = address;
        order.totalPrice = totalPrice;
        order.status = OrderStatus.PENDING;
        return order;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    /**
     * 주문 취소 (ADR-04-011).
     */
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 결제 완료 (Phase 5).
     */
    public void markAsPaid() {
        this.status = OrderStatus.PAID;
    }
}
