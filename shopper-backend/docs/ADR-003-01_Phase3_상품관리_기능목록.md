Phase 3: 상품 관리 기능 목록

3.1 엔티티 설계 (3개)

| 엔티티          | 주요 필드                                                 | 설명                           |
  |--------------|-------------------------------------------------------|------------------------------|
| Category     | id, parent(self-join), name, depth                    | 계층형 카테고리 (예: 전자제품 > 스마트폰)    |
| Product      | id, category, name, description, price, stock, status | 상품 정보 (ACTIVE/INACTIVE)      |
| ProductImage | id, product, url, isMain, sortOrder                   | 상품 이미지 (S3 URL, 대표 이미지 + 순서) |

관계:
Category ─(self-join)─ Category   (1:N, 계층형)
Category ────────────── Product   (1:N)
Product ─────────────── ProductImage (1:N)

  ---
3.2 Repository + DTO (6개)

Repository (3개):
- CategoryRepository.java — 카테고리 조회, 계층형 쿼리
- ProductRepository.java — 상품 CRUD, QueryDSL 동적 검색
- ProductImageRepository.java — 이미지 조회/삭제

DTO (3개):
- ProductRequest.java — 상품 등록/수정 요청 (@Valid)
- ProductResponse.java — 상품 상세 응답 (이미지 포함)
- ProductListResponse.java — 상품 목록 응답 (간략 정보)

  ---
3.3 공개 상품 조회 API (2개)

| Method | Endpoint           | 인증  | 설명                             |
  |--------|--------------------|-----|--------------------------------|
| GET    | /api/products      | ❌   | 상품 목록 (페이징 + 카테고리 필터 + 키워드 검색) |
| GET    | /api/products/{id} | ❌   | 상품 상세 (이미지, 재고 포함)             |

QueryDSL 동적 쿼리:
- 카테고리 필터 (?categoryId=1)
- 키워드 검색 (?keyword=노트북 → name/description LIKE)
- 가격 범위 (?minPrice=10000&maxPrice=50000)
- 재고 상태 (?inStock=true)
- 페이징 (?page=0&size=20)

  ---
3.4 관리자 상품 관리 API (4개)

| Method | Endpoint                        | 인증       | 설명                              |
  |--------|---------------------------------|----------|---------------------------------|
| POST   | /api/admin/products             | 🔒 Admin | 상품 등록                           |
| PUT    | /api/admin/products/{id}        | 🔒 Admin | 상품 수정                           |
| DELETE | /api/admin/products/{id}        | 🔒 Admin | 상품 삭제 (소프트 삭제: status=INACTIVE) |
| POST   | /api/admin/products/{id}/images | 🔒 Admin | 상품 이미지 업로드 (S3)                 |

  ---
3.5 S3 이미지 업로드 (1개)

구현 위치: infra/s3/S3Uploader.java

기능:
- MultipartFile → S3 업로드
- 파일명 UUID 생성 (중복 방지)
- 업로드 성공 → S3 URL 반환
- 지원 형식: .jpg, .jpeg, .png, .webp
- 최대 파일 크기: 5MB

설정:
- application-local.yaml / application-prod.yaml에 S3 bucket/region 추가
- AWS SDK v2 사용 (BOM 이미 추가됨)

  ---
3.6 카테고리 API (선택, 관리자용)

| Method | Endpoint                   | 인증       | 설명                     |
  |--------|----------------------------|----------|------------------------|
| GET    | /api/categories            | ❌        | 카테고리 계층 구조 조회 (트리 형태)  |
| POST   | /api/admin/categories      | 🔒 Admin | 카테고리 생성                |
| PUT    | /api/admin/categories/{id} | 🔒 Admin | 카테고리 수정                |
| DELETE | /api/admin/categories/{id} | 🔒 Admin | 카테고리 삭제 (하위 상품 있으면 거부) |

  ---
구현 순서 (권장)

1. 엔티티 + Repository — Category, Product, ProductImage
2. DTO — ProductRequest, ProductResponse, ProductListResponse
3. ProductService (조회) — 목록 조회, 상세 조회 (QueryDSL 동적 쿼리)
4. ProductController (공개 API) — GET /api/products, GET /api/products/{id}
5. S3Uploader — 이미지 업로드 인프라 구현
6. ProductService (관리자) — 상품 등록/수정/삭제, 이미지 업로드
7. ProductController (관리자 API) — POST/PUT/DELETE /api/admin/products, 이미지 업로드
8. (선택) CategoryService + CategoryController — 카테고리 관리 API

  ---
총 구현 파일 수: 약 10~12개 (카테고리 API 포함 시 14~16개)