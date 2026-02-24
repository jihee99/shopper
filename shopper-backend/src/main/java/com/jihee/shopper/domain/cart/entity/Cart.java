package com.jihee.shopper.domain.cart.entity;

import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 장바구니 엔티티 (ADR-04-001).
 *
 * <p>사용자당 1:1 관계로 최초 접근 시 자동 생성된다.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    /**
     * 사용자를 위한 장바구니 생성 (ADR-04-001).
     */
    public static Cart createForUser(User user) {
        Cart cart = new Cart();
        cart.user = user;
        return cart;
    }
}
