package com.jihee.shopper.domain.order;

import com.jihee.shopper.domain.cart.CartItemRepository;
import com.jihee.shopper.domain.cart.CartRepository;
import com.jihee.shopper.domain.cart.entity.Cart;
import com.jihee.shopper.domain.cart.entity.CartItem;
import com.jihee.shopper.domain.order.dto.OrderRequest;
import com.jihee.shopper.domain.order.dto.OrderResponse;
import com.jihee.shopper.domain.order.entity.Order;
import com.jihee.shopper.domain.order.entity.OrderStatus;
import com.jihee.shopper.domain.product.CategoryRepository;
import com.jihee.shopper.domain.product.ProductRepository;
import com.jihee.shopper.domain.product.entity.Category;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.domain.user.AddressRepository;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.Address;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Address testAddress;
    private Product testProduct1;
    private Product testProduct2;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser("test@example.com", "Test User");
        userRepository.save(testUser);

        testAddress = Address.of(
                testUser, "집", "홍길동", "010-1234-5678",
                "12345", "서울특별시 강남구", "101동 101호", true
        );
        addressRepository.save(testAddress);

        testCategory = Category.createRoot("전자제품");
        categoryRepository.save(testCategory);

        testProduct1 = Product.create(testCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        testProduct2 = Product.create(testCategory, "LG 그램", "초경량 노트북", 1800000, 5);
        productRepository.save(testProduct1);
        productRepository.save(testProduct2);
    }

    // ── 주문 생성 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("주문 생성 성공 - 재고 차감, OrderItem 스냅샷, 장바구니 비우기")
    void createOrder_Success() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem1 = CartItem.of(cart, testProduct1, 2);
        CartItem cartItem2 = CartItem.of(cart, testProduct2, 1);
        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);

        OrderRequest request = new OrderRequest(
                testAddress.getId(),
                List.of(cartItem1.getId(), cartItem2.getId())
        );

        // when
        OrderResponse response = orderService.createOrder(testUser.getId(), request);

        // then
        // 1. 주문 생성 확인
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalPrice()).isEqualTo(6800000);  // 2500000*2 + 1800000*1
        assertThat(response.getOrderItems()).hasSize(2);

        // 2. 재고 차감 확인
        Product updatedProduct1 = productRepository.findById(testProduct1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(testProduct2.getId()).orElseThrow();
        assertThat(updatedProduct1.getStock()).isEqualTo(8);  // 10 - 2
        assertThat(updatedProduct2.getStock()).isEqualTo(4);  // 5 - 1

        // 3. OrderItem 스냅샷 확인
        assertThat(response.getOrderItems().get(0).getPrice()).isIn(2500000, 1800000);
        assertThat(response.getOrderItems().get(0).getProductName()).isIn("맥북 프로", "LG 그램");

        // 4. 장바구니 비우기 확인
        assertThat(cartItemRepository.findByCartId(cart.getId())).isEmpty();
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_Fail_OutOfStock() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 15);  // 재고 10개
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(
                testAddress.getId(),
                List.of(cartItem.getId())
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(testUser.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);

        // 재고는 차감되지 않아야 함 (트랜잭션 롤백)
        Product product = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("주문 생성 실패 - INACTIVE 상품")
    void createOrder_Fail_InactiveProduct() {
        // given
        testProduct1.deactivate();

        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(
                testAddress.getId(),
                List.of(cartItem.getId())
        );

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(testUser.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    // ── 주문 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("내 주문 내역 조회 성공")
    void getMyOrders_Success() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem1 = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem1);

        OrderRequest request1 = new OrderRequest(testAddress.getId(), List.of(cartItem1.getId()));
        orderService.createOrder(testUser.getId(), request1);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<OrderResponse> responses = orderService.getMyOrders(testUser.getId(), pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrder_Success() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(testAddress.getId(), List.of(cartItem.getId()));
        OrderResponse created = orderService.createOrder(testUser.getId(), request);

        // when
        OrderResponse response = orderService.getOrder(testUser.getId(), created.getOrderId());

        // then
        assertThat(response.getOrderId()).isEqualTo(created.getOrderId());
        assertThat(response.getTotalPrice()).isEqualTo(5000000);
        assertThat(response.getOrderItems()).hasSize(1);
    }

    // ── 주문 취소 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("주문 취소 성공 - PENDING 상태, 재고 복구")
    void cancelOrder_Success_PendingStatus() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 3);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(testAddress.getId(), List.of(cartItem.getId()));
        OrderResponse created = orderService.createOrder(testUser.getId(), request);

        // 주문 전 재고: 10, 주문 후 재고: 7
        Product afterOrder = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(afterOrder.getStock()).isEqualTo(7);

        // when
        orderService.cancelOrder(testUser.getId(), created.getOrderId());

        // then
        Order cancelled = orderRepository.findById(created.getOrderId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 재고 복구 확인: 7 + 3 = 10
        Product afterCancel = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(afterCancel.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("주문 취소 실패 - PAID 상태")
    void cancelOrder_Fail_PaidStatus() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(testAddress.getId(), List.of(cartItem.getId()));
        OrderResponse created = orderService.createOrder(testUser.getId(), request);

        // 주문 상태를 PAID로 변경
        Order order = orderRepository.findById(created.getOrderId()).orElseThrow();
        order.markAsPaid();

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(testUser.getId(), created.getOrderId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_ALREADY_PAID);
    }

    @Test
    @DisplayName("주문 취소 실패 - 이미 취소된 주문")
    void cancelOrder_Fail_AlreadyCancelled() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(testAddress.getId(), List.of(cartItem.getId()));
        OrderResponse created = orderService.createOrder(testUser.getId(), request);

        // 첫 번째 취소
        orderService.cancelOrder(testUser.getId(), created.getOrderId());

        // when & then - 두 번째 취소 시도
        assertThatThrownBy(() -> orderService.cancelOrder(testUser.getId(), created.getOrderId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
    }

    // ── OrderItem 스냅샷 검증 ───────────────────────────────────────────

    @Test
    @DisplayName("OrderItem 스냅샷 - 상품 가격 변경 후에도 주문 가격 유지")
    void orderItem_PriceSnapshot() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct1, 2);
        cartItemRepository.save(cartItem);

        OrderRequest request = new OrderRequest(testAddress.getId(), List.of(cartItem.getId()));
        OrderResponse created = orderService.createOrder(testUser.getId(), request);

        // 주문 후 상품 가격 변경
        testProduct1.update(testCategory, "맥북 프로", "고성능 노트북", 3000000, 7);
        productRepository.save(testProduct1);

        // when
        OrderResponse response = orderService.getOrder(testUser.getId(), created.getOrderId());

        // then
        // OrderItem의 가격은 주문 시점의 가격(2500000)으로 유지되어야 함
        assertThat(response.getOrderItems().get(0).getPrice()).isEqualTo(2500000);
        assertThat(response.getTotalPrice()).isEqualTo(5000000);  // 2500000 * 2

        // 상품의 현재 가격은 변경되었는지 확인
        Product updatedProduct = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(updatedProduct.getPrice()).isEqualTo(3000000);
    }
}
