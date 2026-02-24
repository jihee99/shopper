package com.jihee.shopper.domain.cart;

import com.jihee.shopper.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 장바구니 리포지토리 (ADR-04-001).
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 사용자 ID로 장바구니 조회.
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * 사용자의 장바구니 존재 여부 확인.
     */
    boolean existsByUserId(Long userId);
}
