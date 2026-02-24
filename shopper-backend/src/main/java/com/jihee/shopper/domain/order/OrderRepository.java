package com.jihee.shopper.domain.order;

import com.jihee.shopper.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * 주문 리포지토리 (ADR-04-014).
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 사용자별 주문 내역 조회 (페이징).
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * 주문 ID와 사용자 ID로 조회 (ADR-04-014: 권한 검증).
     */
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    /**
     * 배송지 사용 여부 확인 (ADR-04-006: Address 삭제 방지).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.address.id = :addressId")
    long countByAddressId(Long addressId);
}
