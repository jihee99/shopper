package com.jihee.shopper.domain.order;

import com.jihee.shopper.domain.cart.CartItemRepository;
import com.jihee.shopper.domain.cart.entity.CartItem;
import com.jihee.shopper.domain.order.dto.OrderRequest;
import com.jihee.shopper.domain.order.dto.OrderResponse;
import com.jihee.shopper.domain.order.entity.Order;
import com.jihee.shopper.domain.order.entity.OrderItem;
import com.jihee.shopper.domain.order.entity.OrderStatus;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.domain.user.AddressRepository;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.Address;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 서비스 (ADR-04-006 ~ ADR-04-014).
 *
 * <p>주문 생성 시 재고 차감, OrderItem 스냅샷 저장, 장바구니 비우기를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    // ── 주문 생성 ──────────────────────────────────────────────────────────

    /**
     * 주문 생성 (ADR-04-010 ~ ADR-04-012).
     *
     * <p>재고 차감, OrderItem 스냅샷, 장바구니 비우기를 단일 트랜잭션에서 처리한다.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        // 1. 검증
        User user = findUserById(userId);
        Address address = findAddressByIdAndUserId(request.getAddressId(), userId);
        List<CartItem> cartItems = validateCartItems(userId, request.getCartItemIds());

        // 2. 재고 검증 및 차감 (ADR-04-010)
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            // 상품 상태 검증
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            // 재고 검증
            if (product.getStock() < quantity) {
                throw new CustomException(ErrorCode.OUT_OF_STOCK);
            }

            // 재고 차감 (낙관적 락)
            product.decreaseStock(quantity);
        }

        // 3. 총액 계산 (ADR-04-008)
        int totalPrice = cartItems.stream()
                .mapToInt(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        // 4. Order 생성
        Order order = Order.create(user, address, totalPrice);
        orderRepository.save(order);

        // 5. OrderItem 생성 (가격 스냅샷, ADR-04-007)
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.of(
                    order,
                    cartItem.getProduct(),
                    cartItem.getQuantity()
            );
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        // 6. 장바구니 비우기 (ADR-04-012)
        cartItemRepository.deleteAll(cartItems);

        return OrderResponse.from(order);
    }

    // ── 주문 조회 ──────────────────────────────────────────────────────────

    /**
     * 내 주문 내역 조회 (페이징, ADR-04-014).
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(OrderResponse::from);
    }

    /**
     * 주문 상세 조회 (ADR-04-014: 권한 검증).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    // ── 주문 취소 ──────────────────────────────────────────────────────────

    /**
     * 주문 취소 (ADR-04-011: PENDING 상태만 재고 복구).
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 결제 완료된 주문은 취소 불가 (환불은 Phase 5)
        if (order.getStatus() == OrderStatus.PAID) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
        }

        // 이미 취소된 주문
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        // PENDING 상태: 재고 복구 (ADR-04-011)
        if (order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                item.getProduct().increaseStock(item.getQuantity());
            }
        }

        // 주문 취소
        order.cancel();
    }

    // ── 내부 공용 ──────────────────────────────────────────────────────────

    /**
     * CartItem 검증 (본인 장바구니, 존재 여부).
     */
    private List<CartItem> validateCartItems(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = new ArrayList<>();

        for (Long cartItemId : cartItemIds) {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

            // 본인 장바구니 확인
            if (!cartItem.getCart().getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
            }

            cartItems.add(cartItem);
        }

        return cartItems;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Address findAddressByIdAndUserId(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
    }
}
