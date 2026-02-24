# ADR-004-01: Phase 4 — 엔티티 설계 (장바구니/주문)

**작성일:** 2026-02-20
**상태:** ✅ 승인됨
**Phase:** 4. 장바구니 / 주문 - 엔티티 설계

---

## 개요

Phase 4의 장바구니와 주문 엔티티 설계를 위한 의사결정을 담는다.
Cart, CartItem, Order, OrderItem 엔티티의 생명주기, 관계, 비즈니스 로직을 정의한다.

**구현 범위:**
- Cart, CartItem 엔티티 및 생명주기
- Order, OrderItem 엔티티 및 주문 흐름
- 재고 차감 및 복구 전략
- 권한 검증 및 보안

---

## ADR-04-001: Cart 생성 시점

### 배경
사용자의 장바구니를 언제 생성할지 결정이 필요하다.

### 결정
**최초 접근 시 자동 생성**

### 근거
- 모든 사용자가 장바구니를 사용하지 않음 (불필요한 레코드 생성 방지)
- 투명함: 사용자가 장바구니 생성을 의식할 필요 없음
- 구현 간단: `getOrCreateCart()` 메서드 한 번으로 처리

### 구현
```java
public Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId)
        .orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            Cart cart = Cart.createForUser(user);
            return cartRepository.save(cart);
        });
}
```

### 대안
- 회원가입 시 생성: 불필요한 레코드 증가
- 명시적 생성: UX 복잡도 증가

---

## ADR-04-002: CartItem 중복 상품 처리

### 배경
같은 상품을 장바구니에 추가할 때 어떻게 처리할지 결정이 필요하다.

### 결정
**수량 증가 (기존 CartItem 업데이트)**

### 근거
- 일반적인 쇼핑몰 UX (쿠팡, 11번가 등)
- 장바구니 UI 단순화 (상품당 1개 아이템)
- DB 레코드 수 최소화

### 구현
```java
public void addToCart(Long userId, Long productId, int quantity) {
    Cart cart = getOrCreateCart(userId);

    // 기존 CartItem 조회
    Optional<CartItem> existing = cartItemRepository
        .findByCartIdAndProductId(cart.getId(), productId);

    if (existing.isPresent()) {
        // 수량 증가
        existing.get().increaseQuantity(quantity);
    } else {
        // 새 CartItem 생성
        Product product = findProductById(productId);
        CartItem cartItem = CartItem.of(cart, product, quantity);
        cartItemRepository.save(cartItem);
    }
}
```

### CartItem 메서드
```java
public void increaseQuantity(int amount) {
    this.quantity += amount;
}
```

---

## ADR-04-003: CartItem 최대 수량 제한

### 배경
장바구니 상품의 최대 수량을 제한할지 결정이 필요하다.

### 결정
**재고와 동일 (stock 이상 불가)**

### 근거
- 주문 시 재고 부족 에러 방지
- 실시간 재고 검증
- 추가 검증 로직 불필요

### 구현
```java
public void addToCart(Long userId, Long productId, int quantity) {
    Product product = findProductById(productId);

    // 재고 검증
    if (quantity > product.getStock()) {
        throw new CustomException(ErrorCode.OUT_OF_STOCK);
    }

    // ... 장바구니 추가 로직
}

public void updateQuantity(Long userId, Long cartItemId, int quantity) {
    CartItem cartItem = findCartItemByIdAndUserId(cartItemId, userId);

    // 재고 검증
    if (quantity > cartItem.getProduct().getStock()) {
        throw new CustomException(ErrorCode.OUT_OF_STOCK);
    }

    if (quantity < 1) {
        throw new CustomException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
    }

    cartItem.updateQuantity(quantity);
}
```

---

## ADR-04-004: 상품 삭제 시 CartItem 처리

### 배경
상품이 INACTIVE 상태가 되면 장바구니에서 어떻게 처리할지 결정이 필요하다.

### 결정
**조회 시 제외 (CartItem은 유지)**

### 근거
- 사용자가 장바구니에 담은 상품이 갑자기 사라지지 않음
- 상품 복구 시 장바구니 유지
- 프론트엔드에서 "품절" 표시 가능

### 구현
```java
public CartResponse getCart(Long userId) {
    Cart cart = getOrCreateCart(userId);
    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

    // ACTIVE 상품만 응답에 포함
    List<CartItemInfo> activeItems = items.stream()
        .filter(item -> item.getProduct().getStatus() == ProductStatus.ACTIVE)
        .map(item -> CartItemInfo.from(item))
        .toList();

    int totalPrice = activeItems.stream()
        .mapToInt(item -> item.getPrice() * item.getQuantity())
        .sum();

    return CartResponse.of(activeItems, totalPrice);
}
```

### 대안
- CASCADE 삭제: 사용자 경험 저하
- Soft Delete 전파: 복잡도 증가

---

## ADR-04-005: 장바구니 만료 정책

### 배경
장바구니 아이템을 일정 기간 후 자동 삭제할지 결정이 필요하다.

### 결정
**만료 없음 (영구 보관)**

### 근거
- 구현 단순화 (배치 작업 불필요)
- 사용자 편의성 (위시리스트 대용)
- DB 용량 문제 적음 (CartItem은 소량 데이터)

### 구현
- CartItem 엔티티에 별도 만료 필드 불필요
- createdAt, updatedAt만 BaseEntity에서 상속

### 대안
- 30/90일 만료: 배치 작업 필요, 사용자 혼란

---

## ADR-04-006: Order 배송지 저장 방식

### 배경
주문 시 배송지 정보를 어떻게 저장할지 결정이 필요하다.

### 결정
**Address 참조 (FK만 저장)**

### 근거
- 정규화 (중복 제거)
- Address 수정 시 주문 내역 영향 없음 (과거 배송지는 변경되지 않음)
- 주문 조회 시 Join 한 번 추가 (성능 영향 미미)

### 구현
```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    // ...
}
```

### 제약사항
**Address 삭제 방지:**
```java
public void deleteAddress(Long userId, Long addressId) {
    Address address = findAddressByIdAndUserId(addressId, userId);

    // 주문에서 사용 중인지 확인
    long orderCount = orderRepository.countByAddressId(addressId);
    if (orderCount > 0) {
        throw new CustomException(ErrorCode.ADDRESS_IN_USE);
    }

    addressRepository.delete(address);
}
```

### ErrorCode 추가
```java
ADDRESS_IN_USE(400, "주문에서 사용 중인 배송지는 삭제할 수 없습니다")
```

---

## ADR-04-007: OrderItem 상품 정보 스냅샷 범위

### 배경
OrderItem에 어떤 상품 정보를 스냅샷으로 저장할지 결정이 필요하다.

### 결정
**가격 + 이름 저장**

### 근거
- 주문 내역 표시에 필요한 최소 정보
- Product 삭제(INACTIVE) 시에도 주문 내역 조회 가능
- DB 용량 절약 (description, image는 Product 참조)

### 구현
```java
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;  // 주문 시점 가격 (스냅샷)

    @Column(nullable = false)
    private String productName;  // 주문 시점 상품명 (스냅샷)

    public static OrderItem of(Order order, Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.product = product;
        item.quantity = quantity;
        item.price = product.getPrice();  // 스냅샷
        item.productName = product.getName();  // 스냅샷
        return item;
    }
}
```

### 대안
- 가격만: 상품 삭제 시 이름 조회 불가
- 전체 정보: DB 용량 증가, 유지보수 복잡

---

## ADR-04-008: Order 총액 저장 방식

### 배경
주문 총액을 어떻게 관리할지 결정이 필요하다.

### 결정
**저장 필드 (Order.totalPrice 컬럼)**

### 근거
- 주문 생성 시점 총액 고정 (배송비, 할인 등 추가 고려 사항)
- 조회 성능 (집계 쿼리 불필요)
- 향후 확장 용이 (배송비, 쿠폰 할인 등)

### 구현
```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Column(nullable = false)
    private Integer totalPrice;  // 주문 총액

    // ...
}
```

**주문 생성 시 총액 계산:**
```java
public OrderResponse createOrder(Long userId, OrderRequest request) {
    // 1. OrderItem 생성
    List<OrderItem> orderItems = createOrderItems(order, cartItems);

    // 2. 총액 계산
    int totalPrice = orderItems.stream()
        .mapToInt(item -> item.getPrice() * item.getQuantity())
        .sum();

    // 3. Order 생성
    Order order = Order.create(user, address, totalPrice);

    return OrderResponse.from(order);
}
```

---

## ADR-04-009: OrderStatus 초기 상태

### 배경
주문 생성 시 초기 상태를 정의해야 한다.

### 결정
**PENDING (결제 대기)**

### 근거
- 명확한 의미 (결제 대기 중)
- 설계 문서 명시 (PENDING / PAID / CANCELLED)
- 일반적인 이커머스 용어

### OrderStatus enum
```java
public enum OrderStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    CANCELLED   // 주문 취소
}
```

### 상태 전이
```
PENDING → PAID (결제 성공, Phase 5)
PENDING → CANCELLED (결제 실패 또는 사용자 취소)
PAID → CANCELLED (환불, Phase 5)
```

### 구현
```java
public static Order create(User user, Address address, int totalPrice) {
    Order order = new Order();
    order.user = user;
    order.address = address;
    order.totalPrice = totalPrice;
    order.status = OrderStatus.PENDING;  // 초기 상태
    return order;
}
```

---

## ADR-04-010: 재고 차감 시점

### 배경
재고를 언제 차감할지 결정이 필요하다.

### 결정
**주문 생성 시 (POST /api/orders)**

### 근거
- 설계 문서 명시 ("주문 생성 시 재고 차감")
- 간단한 흐름 (결제 실패 시 재고 복구)
- 동시성 제어 명확 (낙관적 락 활용)

### 구현
```java
@Transactional
public OrderResponse createOrder(Long userId, OrderRequest request) {
    // 1. 검증
    List<CartItem> cartItems = validateCartItems(userId, request.getCartItemIds());

    // 2. 재고 검증 및 차감
    for (CartItem cartItem : cartItems) {
        Product product = cartItem.getProduct();
        int quantity = cartItem.getQuantity();

        if (product.getStock() < quantity) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        product.decreaseStock(quantity);  // 낙관적 락 활용
    }

    // 3. Order + OrderItem 생성
    // ...
}
```

### 결제 실패 시 처리 (Phase 5)
```java
public void cancelOrder(Long orderId) {
    Order order = findOrderById(orderId);

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
    }

    // 재고 복구
    for (OrderItem item : order.getOrderItems()) {
        item.getProduct().increaseStock(item.getQuantity());
    }

    order.cancel();  // status = CANCELLED
}
```

---

## ADR-04-011: 주문 취소 시 재고 복구

### 배경
주문 취소 시 재고를 복구할지 결정이 필요하다.

### 결정
**조건부 복구 (PENDING 상태만 복구)**

### 근거
- PENDING 취소: 결제 전이므로 즉시 재고 복구
- PAID 취소: 환불 프로세스(Phase 5)에서 처리
- 명확한 책임 분리

### 구현
```java
public void cancelOrder(Long userId, Long orderId) {
    Order order = findOrderByIdAndUserId(orderId, userId);

    if (order.getStatus() == OrderStatus.PAID) {
        throw new CustomException(ErrorCode.ORDER_ALREADY_PAID);
    }

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
    }

    // PENDING 상태: 재고 복구
    for (OrderItem item : order.getOrderItems()) {
        item.getProduct().increaseStock(item.getQuantity());
    }

    order.cancel();
}
```

### Order 메서드
```java
public void cancel() {
    this.status = OrderStatus.CANCELLED;
}
```

### ErrorCode 추가
```java
ORDER_ALREADY_PAID(400, "이미 결제 완료된 주문입니다")
```

---

## ADR-04-012: 장바구니 비우기 시점

### 배경
주문 생성 시 장바구니를 언제 비울지 결정이 필요하다.

### 결정
**주문 생성 시 (선택된 CartItem만 삭제)**

### 근거
- 설계 문서 명시
- 일반적인 UX (주문하면 장바구니 비워짐)
- 중복 주문 방지

### 구현
```java
@Transactional
public OrderResponse createOrder(Long userId, OrderRequest request) {
    // 1. CartItem 검증
    List<CartItem> cartItems = validateCartItems(userId, request.getCartItemIds());

    // 2. 재고 차감
    // ...

    // 3. Order + OrderItem 생성
    // ...

    // 4. 선택된 CartItem 삭제
    cartItemRepository.deleteAll(cartItems);

    return OrderResponse.from(order);
}
```

### 부분 주문 지원
**OrderRequest:**
```java
@Getter
@NoArgsConstructor
public class OrderRequest {
    @NotNull(message = "배송지는 필수입니다")
    private Long addressId;

    @NotEmpty(message = "주문할 상품을 선택해주세요")
    private List<Long> cartItemIds;  // 선택된 CartItem ID 목록
}
```

---

## ADR-04-013: 주문 번호 생성 전략

### 배경
주문 번호를 어떻게 생성할지 결정이 필요하다.

### 결정
**DB Auto Increment (id 사용)**

### 근거
- 구현 단순 (별도 로직 불필요)
- 성능 우수 (인덱싱)
- 순차 번호로 관리 용이

### 구현
```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 주문 번호로 사용

    // ...
}
```

### 보안
- 외부 노출 시 고려사항 (Phase 6 관리자 기능)
- 주문 조회는 본인 확인 필수 (`findByIdAndUserId`)

### 대안
- UUID: 길이 증가, 성능 저하
- 커스텀 포맷: 추가 로직 필요

---

## ADR-04-014: 주문 조회 권한

### 배경
주문 조회 시 권한을 어떻게 검증할지 결정이 필요하다.

### 결정
**Repository 레벨 (`findByIdAndUserId`)**

### 근거
- 데이터 접근 계층에서 필터링 (안전)
- 쿼리 최적화 (WHERE 조건)
- 일관성 (Address, CartItem과 동일 패턴)

### 구현
**OrderRepository:**
```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
    Page<Order> findByUserId(Long userId, Pageable pageable);
}
```

**OrderService:**
```java
public OrderResponse getOrder(Long userId, Long orderId) {
    Order order = orderRepository.findByIdAndUserId(orderId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    return OrderResponse.from(order);
}

public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
    Page<Order> orders = orderRepository.findByUserId(userId, pageable);
    return orders.map(OrderResponse::from);
}
```

---

## ADR-04-015: CartItem-Product 연관관계 전략

### 배경
Product가 삭제(INACTIVE)되었을 때 CartItem을 어떻게 처리할지 결정이 필요하다.

### 결정
**참조 유지 (조회 시 필터링)**

### 근거
- 소프트 삭제 정책과 일관성
- 상품 복구 시 장바구니 유지
- 사용자 경험: "품절" 안내 가능

### 구현
**CartItem 엔티티:**
```java
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Product 삭제 시 CASCADE 하지 않음 (기본값)
}
```

**CartService 필터링:**
```java
public CartResponse getCart(Long userId) {
    Cart cart = getOrCreateCart(userId);
    List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

    // ACTIVE 상품만 포함
    List<CartItemInfo> activeItems = items.stream()
        .filter(item -> item.getProduct().getStatus() == ProductStatus.ACTIVE)
        .map(CartItemInfo::from)
        .toList();

    return CartResponse.of(activeItems, calculateTotal(activeItems));
}
```

**프론트엔드 안내:**
- INACTIVE 상품: "품절된 상품입니다" 메시지
- 주문 불가, 삭제 버튼만 제공

---

## 엔티티 설계 요약

### Cart
```java
- id: Long
- user: User (ManyToOne, 1:1 관계)
- createdAt, updatedAt (BaseEntity)
```

### CartItem
```java
- id: Long
- cart: Cart (ManyToOne)
- product: Product (ManyToOne)
- quantity: Integer
- createdAt, updatedAt (BaseEntity)
```

### Order
```java
- id: Long (주문 번호)
- user: User (ManyToOne)
- address: Address (ManyToOne, 삭제 방지)
- totalPrice: Integer
- status: OrderStatus (PENDING/PAID/CANCELLED)
- createdAt, updatedAt (BaseEntity)
```

### OrderItem
```java
- id: Long
- order: Order (ManyToOne)
- product: Product (ManyToOne)
- quantity: Integer
- price: Integer (스냅샷)
- productName: String (스냅샷)
- createdAt, updatedAt (BaseEntity)
```

### OrderStatus (enum)
```java
PENDING, PAID, CANCELLED
```

---

## ErrorCode 추가 필요

```java
ADDRESS_IN_USE(400, "주문에서 사용 중인 배송지는 삭제할 수 없습니다")
ORDER_ALREADY_PAID(400, "이미 결제 완료된 주문입니다")
```

---

## 구현 순서

1. **OrderStatus enum 작성**
2. **Cart, CartItem 엔티티 작성**
3. **Order, OrderItem 엔티티 작성**
4. **Repository 작성** (4개)
5. **DTO 작성** (4개)
6. **CartService 작성** (CRUD + 중복 상품 처리)
7. **OrderService 작성** (주문 생성 + 재고 차감 + 장바구니 비우기)
8. **Controller 작성** (2개)

---

**문서 버전:** 1.0
**다음 문서:** 구현 완료 후 Phase 5 (결제 연동) ADR 작성 예정
