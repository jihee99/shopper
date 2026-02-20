# ADR-003: Phase 3 — 상품 관리

**작성일:** 2026-02-20
**상태:** ✅ 승인됨
**Phase:** 3. 상품 관리 (Product Management)

---

## 개요

Phase 3에서는 쇼핑몰의 핵심인 상품 관리 기능을 구현한다.
계층형 카테고리, 상품 CRUD, 이미지 관리(S3 업로드), QueryDSL 기반 검색/필터링을 포함한다.

**구현 범위:**
- Category, Product, ProductImage 엔티티
- 공개 상품 조회 API (페이징, 필터, 검색)
- 관리자 상품 관리 API
- S3 이미지 업로드

---

## ADR-03-001: Category 계층 구조 깊이 제한

### 배경
카테고리는 계층형 구조(self-join)로 설계되며, 무제한 깊이는 관리 복잡도와 성능 문제를 야기할 수 있다.

### 결정
**3단계 계층**으로 제한한다.

```
depth 0: 대분류 (예: 전자제품)
depth 1: 중분류 (예: 스마트폰)
depth 2: 소분류 (예: 갤럭시 시리즈)
```

### 근거
- 일반적인 쇼핑몰 카테고리 구조와 일치 (쿠팡, 11번가 등)
- UI/UX에서 3단계가 사용자 탐색에 적절
- 재귀 쿼리 성능 부담 최소화

### 구현
- `Category.depth` 필드 (Integer, 0~2)
- 카테고리 생성 시 depth 검증 로직
- `depth >= 3`인 경우 `ErrorCode.CATEGORY_DEPTH_EXCEEDED` 예외 발생

---

## ADR-03-002: Product 가격 타입

### 배경
가격 데이터를 저장할 자료형 선택이 필요하다. 정밀도, 성능, 통화 단위를 고려해야 한다.

### 결정
**Integer 타입 (원 단위)** 사용

### 근거
- 한국 쇼핑몰은 원(₩) 단위, 소수점 불필요
- BigDecimal 대비 성능 우수 (인덱싱, 비교 연산 빠름)
- 최대값: 2,147,483,647원 (약 21억 원) — 일반 상품 가격 충분

### 구현
```java
@Column(nullable = false)
private Integer price;  // 단위: 원(₩)
```

### 제약사항
- 가격은 항상 양수 (`price > 0`)
- 프론트엔드에서 통화 포맷 처리 (예: `10000` → `"10,000원"`)

---

## ADR-03-003: Product 재고 동시성 제어

### 배경
여러 사용자가 동시에 같은 상품을 주문할 때 재고 차감 충돌이 발생할 수 있다.

### 결정
**낙관적 락 (@Version)** 사용

### 근거
- 단일 서버 환경에서 충분한 성능
- 재고 충돌은 상대적으로 드묾 (일반 쇼핑몰 규모)
- 비관적 락 대비 처리량 우수

### 구현
```java
@Version
private Long version;

@Column(nullable = false)
private Integer stock;
```

- 재고 차감 시 `OptimisticLockException` 발생 시 재시도 (최대 3회)
- 재시도 실패 시 `ErrorCode.OUT_OF_STOCK` 응답

### 대안
- Phase 5 (배포) 단계에서 다중 서버 환경 시 Redis 분산 락 고려

---

## ADR-03-004: Product status 관리 방식

### 배경
상품 삭제를 하드 삭제할 것인지, 상태 플래그로 관리할 것인지 결정이 필요하다.

### 결정
**소프트 삭제 (status enum)** 사용

```java
public enum ProductStatus {
    ACTIVE,    // 판매 중
    INACTIVE   // 판매 중지 (삭제 포함)
}
```

### 근거
- 주문 이력에서 상품 정보 참조 필요 (OrderItem이 Product를 참조)
- 삭제된 상품 복구 가능성
- 통계 및 분석 데이터 보존

### 구현
- 관리자 삭제 API → `status = INACTIVE` 처리
- 상품 목록 조회 시 `status = ACTIVE` 필터링
- QueryDSL: `product.status.eq(ProductStatus.ACTIVE)`

---

## ADR-03-005: ProductImage 메인 이미지 관리

### 배경
상품당 여러 이미지가 있으며, 대표 이미지를 명확히 구분해야 한다.

### 결정
**isMain 플래그 + 서비스 검증** 방식

### 근거
- 명시적: 어떤 이미지가 대표인지 명확
- 유연함: sortOrder와 독립적으로 관리 가능

### 구현
```java
@Column(nullable = false)
private boolean isMain;  // 대표 이미지 여부

@Column(nullable = false)
private Integer sortOrder;  // 정렬 순서 (0, 1, 2, ...)
```

**검증 로직:**
- 이미지 등록 시 `isMain=true`이면 기존 메인 이미지를 `isMain=false`로 변경
- 상품당 메인 이미지는 **정확히 1개** 보장

---

## ADR-03-006: ProductImage 삭제 전략

### 배경
이미지 삭제 시 S3에 저장된 파일도 함께 삭제할 것인지 결정이 필요하다.

### 결정
**즉시 삭제 (DB + S3 동시 삭제)**

### 근거
- 불필요한 S3 스토리지 비용 절감
- 이미지는 복구 필요성이 낮음 (재업로드 가능)
- 단순한 구현

### 구현
```java
// ProductService
public void deleteImage(Long imageId) {
    ProductImage image = imageRepository.findById(imageId)...;
    s3Uploader.delete(image.getUrl());  // S3 삭제
    imageRepository.delete(image);       // DB 삭제
}
```

### 트랜잭션 처리
- S3 삭제 실패 시 DB 삭제도 롤백 (예외 발생)
- `@Transactional` 내에서 S3 삭제 먼저 수행

---

## ADR-03-007: Product description 타입

### 배경
상품 설명은 길이가 가변적이며, 검색/인덱싱 여부를 고려해야 한다.

### 결정
**TEXT 타입** 사용

### 근거
- VARCHAR(1000)으로는 긴 설명 제한적
- LONGTEXT는 과도 (HTML 에디터 사용 시 고려)
- 상품 검색은 주로 `name` 기반, description은 보조

### 구현
```java
@Column(nullable = false)
private String name;  // VARCHAR(255)

@Column(columnDefinition = "TEXT")
private String description;  // TEXT
```

### 검색 전략
- 키워드 검색: `name LIKE %keyword%` 우선
- description 검색은 선택적 (QueryDSL 조건 추가 시)

---

## ADR-03-008: Category 삭제 정책

### 배경
카테고리 삭제 시 하위 상품이 존재하는 경우 처리 방법이 필요하다.

### 결정
**삭제 거부 (예외 발생)**

### 근거
- 데이터 무결성 보장
- 의도하지 않은 대량 상품 삭제 방지
- 관리자가 명시적으로 상품 이동 후 삭제하도록 유도

### 구현
```java
public void deleteCategory(Long categoryId) {
    long productCount = productRepository.countByCategoryId(categoryId);
    if (productCount > 0) {
        throw new CustomException(ErrorCode.CATEGORY_HAS_PRODUCTS);
    }
    categoryRepository.deleteById(categoryId);
}
```

### ErrorCode 추가
```java
CATEGORY_HAS_PRODUCTS(400, "하위 상품이 존재하여 삭제할 수 없습니다")
```

---

## ADR-03-009: Product 판매 수량 추적

### 배경
인기 상품 정렬, 베스트 상품 표시를 위해 판매 수량 추적이 필요할 수 있다.

### 결정
**salesCount 필드 추가**

### 근거
- 집계 쿼리 없이 정렬 가능 (성능 우수)
- 간단한 구현 (`ORDER BY salesCount DESC`)
- 실시간 업데이트 가능

### 구현
```java
@Column(nullable = false)
private Integer salesCount = 0;  // 판매 수량 (기본값 0)
```

**업데이트 시점:**
- 주문 생성 시 (OrderService) `product.increaseSalesCount(quantity)` 호출
- 주문 취소 시 `product.decreaseSalesCount(quantity)` 호출

### 메서드
```java
public void increaseSalesCount(int quantity) {
    this.salesCount += quantity;
}

public void decreaseSalesCount(int quantity) {
    this.salesCount = Math.max(0, this.salesCount - quantity);
}
```

---

## 엔티티 설계 요약

### Category
```java
- id: Long
- parent: Category (self-join, nullable)
- name: String
- depth: Integer (0~2)
- createdAt, updatedAt (BaseEntity)
```

### Product
```java
- id: Long
- category: Category (ManyToOne)
- name: String
- description: String (TEXT)
- price: Integer (원 단위)
- stock: Integer
- status: ProductStatus (ACTIVE, INACTIVE)
- salesCount: Integer (판매 수량)
- version: Long (@Version, 낙관적 락)
- createdAt, updatedAt (BaseEntity)
```

### ProductImage
```java
- id: Long
- product: Product (ManyToOne)
- url: String (S3 URL)
- isMain: boolean (대표 이미지)
- sortOrder: Integer (정렬 순서)
- createdAt, updatedAt (BaseEntity)
```

---

## 구현 순서

1. **엔티티 작성** — Category, Product, ProductImage
2. **Repository 작성** — CategoryRepository, ProductRepository, ProductImageRepository
3. **DTO 작성** — ProductRequest, ProductResponse, ProductListResponse
4. **ProductService (조회)** — 목록/상세 조회, QueryDSL 동적 쿼리
5. **ProductController (공개)** — GET /api/products, GET /api/products/{id}
6. **S3Uploader** — 이미지 업로드 인프라
7. **ProductService (관리자)** — CRUD, 이미지 업로드
8. **ProductController (관리자)** — POST/PUT/DELETE /api/admin/products

---

**문서 버전:** 1.0
**다음 문서:** 구현 완료 후 Phase 4 (장바구니/주문) ADR 작성 예정
