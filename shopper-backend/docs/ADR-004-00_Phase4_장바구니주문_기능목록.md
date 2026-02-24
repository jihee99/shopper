# ADR-004: Phase 4 — 장바구니 / 주문 기능 목록

**작성일:** 2026-02-20
**상태:** ✅ 승인됨
**Phase:** 4. 장바구니 / 주문 (Cart & Order)

---

## 개요

Phase 4에서는 사용자의 쇼핑 흐름 중 장바구니와 주문 생성 기능을 구현한다.
장바구니에 상품을 담고, 주문을 생성하며, 재고를 차감하는 핵심 비즈니스 로직을 포함한다.

**구현 범위:**
- Cart, CartItem, Order, OrderItem 엔티티
- 장바구니 CRUD API
- 주문 생성 및 조회 API
- 재고 차감 로직

**제외 범위:**
- 결제 연동 (Phase 5에서 처리)
- 주문 취소/환불 (Phase 5 결제 취소와 연동)

---

## 4.1 엔티티 설계 (4개)

| 엔티티 | 주요 필드 | 설명 |
|--------|-----------|------|
| **Cart** | id, user | 장바구니 (사용자당 1개, 1:1) |
| **CartItem** | id, cart, product, quantity | 장바구니 상품 |
| **Order** | id, user, totalPrice, status, address | 주문 (PENDING/PAID/CANCELLED) |
| **OrderItem** | id, order, product, quantity, price | 주문 상품 스냅샷 (가격 저장) |

**관계:**
```
User ───(1:1)─── Cart
Cart ───(1:N)─── CartItem
CartItem ─(N:1)─ Product

User ───(1:N)─── Order
Order ──(1:N)─── OrderItem
OrderItem ─(N:1)─ Product
Order ──(N:1)─── Address (배송지)
```

---

## 4.2 Repository + DTO (8개)

### Repository (4개)

| Repository | 주요 메서드 |
|------------|-------------|
| **CartRepository** | `findByUserId()`, `existsByUserId()` |
| **CartItemRepository** | `findByCartId()`, `findByCartIdAndProductId()`, `deleteByCartId()` |
| **OrderRepository** | `findByUserId()`, `findByIdAndUserId()` |
| **OrderItemRepository** | `findByOrderId()` |

### DTO (4개)

| DTO | 용도 |
|-----|------|
| **CartItemRequest** | 장바구니 상품 추가/수정 (productId, quantity) |
| **CartResponse** | 장바구니 조회 응답 (상품 목록 + 총 금액) |
| **OrderRequest** | 주문 생성 요청 (addressId, cartItemIds[]) |
| **OrderResponse** | 주문 상세 응답 (주문 정보 + OrderItem 목록) |

---

## 4.3 장바구니 API (4개)

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| `GET` | `/api/cart` | ✅ | 장바구니 조회 (상품 목록 + 총 금액) |
| `POST` | `/api/cart/items` | ✅ | 장바구니 상품 추가 (중복 시 수량 증가) |
| `PUT` | `/api/cart/items/{id}` | ✅ | 수량 변경 (1 이상) |
| `DELETE` | `/api/cart/items/{id}` | ✅ | 상품 삭제 |

**비즈니스 로직:**
- 사용자당 장바구니 1개 (최초 접근 시 자동 생성)
- 같은 상품 추가 시 → 수량 증가 (새 CartItem 생성 X)
- 수량 0 이하 → 에러 (`CART_ITEM_QUANTITY_INVALID`)
- 장바구니 조회 시 총 금액 계산 (sum of `product.price * quantity`)

---

## 4.4 주문 API (3개)

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| `POST` | `/api/orders` | ✅ | 주문 생성 (재고 차감, OrderItem 스냅샷) |
| `GET` | `/api/orders/me` | ✅ | 내 주문 내역 (페이징) |
| `GET` | `/api/orders/{id}` | ✅ | 주문 상세 (OrderItem 포함) |

**주문 생성 흐름 (POST /api/orders):**
```
1. 요청 검증: addressId, cartItemIds[] 존재 확인
2. CartItem 조회 및 검증 (본인 장바구니, 상품 ACTIVE 상태)
3. 재고 검증: product.stock >= cartItem.quantity
4. 주문 생성:
   - Order 엔티티 생성 (status = PENDING, totalPrice 계산)
   - OrderItem 생성 (상품 가격 스냅샷 저장)
5. 재고 차감: product.decreaseStock(quantity)
6. 장바구니 비우기: CartItem 삭제
7. Order 반환
```

**주문 상태 (OrderStatus enum):**
```java
PENDING    // 주문 생성 (결제 대기)
PAID       // 결제 완료
CANCELLED  // 주문 취소
```

---

## 4.5 핵심 비즈니스 로직

### 4.5.1 장바구니 중복 상품 처리

**시나리오:** 사용자가 이미 장바구니에 있는 상품을 다시 추가

**처리:**
- 기존 CartItem 조회 (`findByCartIdAndProductId`)
- 있으면 → 수량 증가 (`cartItem.increaseQuantity()`)
- 없으면 → 새 CartItem 생성

### 4.5.2 재고 차감 동시성 제어

**문제:** 여러 사용자가 동시에 같은 상품 주문 시 재고 부족 발생 가능

**해결:** Product 엔티티의 낙관적 락 활용 (ADR-03-003: `@Version`)
- 재고 차감 시 `OptimisticLockException` 발생 시 → `OUT_OF_STOCK` 에러
- 재시도 없음 (주문 실패 응답)

### 4.5.3 주문 가격 스냅샷

**목적:** 주문 당시 상품 가격 보존 (이후 가격 변경 시에도 주문 내역 정확)

**구현:**
```java
OrderItem orderItem = OrderItem.of(
    order,
    product,
    quantity,
    product.getPrice()  // 주문 시점 가격 저장
);
```

### 4.5.4 주문 총액 계산

**계산:**
```java
int totalPrice = orderItems.stream()
    .mapToInt(item -> item.getPrice() * item.getQuantity())
    .sum();
```

**검증:** OrderItem 가격 합계 = Order.totalPrice (서버 검증)

---

## 4.6 예외 처리

### 장바구니 관련 에러

| ErrorCode | HTTP | 메시지 |
|-----------|------|--------|
| `CART_ITEM_NOT_FOUND` | 404 | 장바구니 상품을 찾을 수 없습니다 |
| `CART_ITEM_QUANTITY_INVALID` | 400 | 수량은 1 이상이어야 합니다 |

### 주문 관련 에러

| ErrorCode | HTTP | 메시지 |
|-----------|------|--------|
| `ORDER_NOT_FOUND` | 404 | 주문을 찾을 수 없습니다 |
| `OUT_OF_STOCK` | 409 | 재고가 부족합니다 |
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없습니다 |
| `ADDRESS_NOT_FOUND` | 404 | 배송지를 찾을 수 없습니다 |

---

## 4.7 보안 고려사항

### 권한 검증

**장바구니:**
- CartItem 수정/삭제 시 → 본인 장바구니인지 검증
- `cartItem.getCart().getUser().getId() == userId`

**주문:**
- 주문 조회 시 → 본인 주문인지 검증
- `order.getUser().getId() == userId`

### 가격 변조 방지

**주문 생성 시:**
- 클라이언트가 전달한 가격 무시
- 서버에서 Product 조회 후 `product.getPrice()` 사용
- OrderItem에 가격 스냅샷 저장

---

## 4.8 트랜잭션 전략

### 주문 생성 트랜잭션 범위

```java
@Transactional
public OrderResponse createOrder(Long userId, OrderRequest request) {
    // 1. 검증 (Address, CartItem, 재고)
    // 2. Order 생성
    // 3. OrderItem 생성
    // 4. 재고 차감 (낙관적 락)
    // 5. CartItem 삭제
    // 모두 성공 시 커밋, 실패 시 롤백
}
```

**주의:** S3 업로드와 달리 외부 API 호출 없음 (DB 트랜잭션만 관리)

---

## 4.9 데이터 일관성

### Cart 자동 생성

**전략:** 사용자 최초 장바구니 접근 시 자동 생성
```java
public Cart getOrCreateCart(Long userId) {
    return cartRepository.findByUserId(userId)
        .orElseGet(() -> {
            Cart cart = Cart.createForUser(user);
            return cartRepository.save(cart);
        });
}
```

### OrderItem과 Product 관계

**관계:** `@ManyToOne` (OrderItem → Product)
- Product 삭제(INACTIVE) 시에도 OrderItem은 유지
- Product 정보는 스냅샷으로 저장 (price, name)

---

## 구현 순서

1. **엔티티 작성** — Cart, CartItem, Order, OrderItem, OrderStatus enum
2. **Repository 작성** — CartRepository, CartItemRepository, OrderRepository, OrderItemRepository
3. **DTO 작성** — CartItemRequest, CartResponse, OrderRequest, OrderResponse
4. **CartService 작성** — 장바구니 CRUD 로직
5. **CartController 작성** — GET/POST/PUT/DELETE /api/cart
6. **OrderService 작성** — 주문 생성 (재고 차감 포함), 조회
7. **OrderController 작성** — POST /api/orders, GET /api/orders/me, GET /api/orders/{id}
8. **ErrorCode 추가** — 필요 시 (대부분 기존 ErrorCode 사용)

---

## 구현 예상 파일 수

| 분류 | 파일 수 | 설명 |
|------|---------|------|
| **엔티티** | 5 | Cart, CartItem, Order, OrderItem, OrderStatus |
| **Repository** | 4 | CartRepository, CartItemRepository, OrderRepository, OrderItemRepository |
| **DTO** | 4 | CartItemRequest, CartResponse, OrderRequest, OrderResponse |
| **Service** | 2 | CartService, OrderService |
| **Controller** | 2 | CartController, OrderController |

**총 파일 수: 17개**

---

## API 엔드포인트 요약

### 장바구니 API (4개)
```
GET    /api/cart              — 장바구니 조회
POST   /api/cart/items        — 상품 추가
PUT    /api/cart/items/{id}   — 수량 변경
DELETE /api/cart/items/{id}   — 상품 삭제
```

### 주문 API (3개)
```
POST /api/orders      — 주문 생성 (재고 차감, 장바구니 비우기)
GET  /api/orders/me   — 내 주문 내역 (페이징)
GET  /api/orders/{id} — 주문 상세
```

---

**문서 버전:** 1.0
**이전 문서:** ADR-003 (상품 관리)
**다음 단계:** 엔티티 설계 의사결정 → 구현 → Phase 5 (결제 연동)
