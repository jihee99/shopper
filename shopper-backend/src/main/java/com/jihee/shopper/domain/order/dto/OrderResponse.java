package com.jihee.shopper.domain.order.dto;

import com.jihee.shopper.domain.order.entity.Order;
import com.jihee.shopper.domain.order.entity.OrderItem;
import com.jihee.shopper.domain.order.entity.OrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 응답 DTO (ADR-04-007: OrderItem 스냅샷 포함).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderResponse {

    private final Long orderId;
    private final OrderStatus status;
    private final Integer totalPrice;
    private final AddressInfo address;
    private final List<OrderItemInfo> orderItems;
    private final LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        List<OrderItemInfo> orderItems = order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .toList();

        AddressInfo addressInfo = new AddressInfo(
                order.getAddress().getId(),
                order.getAddress().getName(),
                order.getAddress().getRecipient(),
                order.getAddress().getPhone(),
                order.getAddress().getZipCode(),
                order.getAddress().getAddress(),
                order.getAddress().getAddressDetail()
        );

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalPrice(),
                addressInfo,
                orderItems,
                order.getCreatedAt()
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class OrderItemInfo {
        private final Long productId;
        private final String productName;  // 스냅샷
        private final Integer price;       // 스냅샷
        private final Integer quantity;

        public static OrderItemInfo from(OrderItem item) {
            return new OrderItemInfo(
                    item.getProduct().getId(),
                    item.getProductName(),  // 스냅샷
                    item.getPrice(),        // 스냅샷
                    item.getQuantity()
            );
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class AddressInfo {
        private final Long addressId;
        private final String name;
        private final String recipient;
        private final String phone;
        private final String zipCode;
        private final String address;
        private final String addressDetail;
    }
}
