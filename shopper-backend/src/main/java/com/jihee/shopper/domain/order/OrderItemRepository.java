package com.jihee.shopper.domain.order;

import com.jihee.shopper.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주문 상품 리포지토리.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 주문 ID로 전체 OrderItem 조회.
     */
    List<OrderItem> findByOrderId(Long orderId);
}
