package com.jihee.shopper.domain.cart;

import com.jihee.shopper.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 상품 리포지토리 (ADR-04-002).
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * 장바구니 ID로 전체 아이템 조회.
     */
    List<CartItem> findByCartId(Long cartId);

    /**
     * 장바구니 ID와 상품 ID로 조회 (ADR-04-002: 중복 상품 확인).
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * 장바구니 전체 비우기.
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(Long cartId);
}
