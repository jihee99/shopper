# ADR-003-03: Phase 3 — 관리자 기능 (S3 업로드 + 상품 관리)

**작성일:** 2026-02-20
**상태:** ✅ 승인됨
**Phase:** 3. 상품 관리 - 관리자 기능

---

## 개요

Phase 3의 관리자 기능으로 S3 이미지 업로드와 상품 CRUD를 구현한다.
이 문서는 S3Uploader 인프라와 관리자 전용 API 구현을 위한 의사결정을 담는다.

**구현 범위:**
- S3 이미지 업로드 인프라 (S3Uploader)
- 관리자 상품 등록/수정/삭제 API
- 관리자 이미지 업로드 API

---

## ADR-03-010: S3 파일명 생성 전략

### 배경
S3에 업로드할 파일명을 어떻게 생성할지 결정이 필요하다. 중복 방지, 보안, 정렬 등을 고려해야 한다.

### 결정
**UUID + 확장자** 방식 사용

### 근거
- 중복 방지 완벽 (UUID는 전역 고유성 보장)
- 파일명 길이 일정 (UUID v4: 36자)
- 보안: 원본 파일명 노출 방지 (사용자 개인정보 포함 가능성)

### 구현 예시
```java
String extension = getExtension(originalFilename);  // "image.jpg" → "jpg"
String fileName = UUID.randomUUID() + "." + extension;
// 결과: "a3f2c1b5-8d4e-4a2f-9e1b-3c5d7e9f1a2b.jpg"
```

### 대안
- 타임스탬프 + UUID: 시간 기반 정렬 가능하지만 파일명 길어짐
- 원본 파일명 + UUID: 원본명 보존하지만 보안 위험

---

## ADR-03-011: S3 경로 구조

### 배경
S3 버킷 내 디렉토리 구조를 어떻게 구성할지 결정이 필요하다.

### 결정
**날짜별 구분** 방식 사용

```
s3://bucket-name/products/YYYY/MM/DD/{UUID}.jpg
```

### 근거
- S3 파티셔닝 최적화 (날짜별 키 분산으로 처리량 향상)
- 관리 용이 (날짜별 백업/삭제, 용량 분석)
- 상품ID 기반 구조의 문제 회피 (상품 생성 전 이미지 업로드 불가)

### 구현 예시
```java
LocalDate now = LocalDate.now();
String path = String.format("products/%d/%02d/%02d/%s",
    now.getYear(), now.getMonthValue(), now.getDayOfMonth(), fileName);
// 결과: "products/2026/02/20/a3f2c1b5-8d4e-4a2f-9e1b-3c5d7e9f1a2b.jpg"
```

### 대안
- 단일 디렉토리: 파일 관리 어려움
- 상품ID별 구분: 상품 생성 전 업로드 불가

---

## ADR-03-012: 이미지 업로드 시점

### 배경
상품 이미지를 언제 업로드할지 결정이 필요하다.

### 결정
**상품 등록 후 별도 업로드**

**API 흐름:**
1. `POST /api/admin/products` — 상품 등록 (이미지 제외)
2. `POST /api/admin/products/{id}/images` — 이미지 업로드 (여러 번 가능)

### 근거
- API 단순화 (JSON과 MultipartFile 분리)
- 이미지 추가/삭제 유연함
- 프론트엔드 UX 개선 (상품 정보 먼저 저장 → 이미지 추가)

### 구현
```java
@PostMapping("/{productId}/images")
public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
    @PathVariable Long productId,
    @RequestParam("file") MultipartFile file,
    @RequestParam(required = false, defaultValue = "false") boolean isMain) {
    // ...
}
```

### 대안
- 동시 업로드: `@RequestPart` 사용, 복잡도 증가
- 사전 업로드: 임시 URL 관리 복잡

---

## ADR-03-013: 메인 이미지 자동 지정 정책

### 배경
첫 번째 이미지 업로드 시 메인 이미지를 자동 지정할지 결정이 필요하다.

### 결정
**첫 이미지 자동 isMain=true**

### 근거
- UX 개선: 첫 이미지는 대부분 메인 이미지
- API 호출 단순화 (명시적 플래그 불필요)
- 명시적 변경 가능 (이후 다른 이미지를 메인으로 변경 가능)

### 구현 로직
```java
List<ProductImage> existingImages = imageRepository.findByProductId(productId);

boolean shouldBeMain = isMain;  // 요청 파라미터
if (existingImages.isEmpty()) {
    shouldBeMain = true;  // 첫 이미지는 무조건 메인
}

if (shouldBeMain) {
    imageRepository.clearMainByProductId(productId);  // 기존 메인 해제
}
```

---

## ADR-03-014: 이미지 개수 제한

### 배경
상품당 이미지 업로드 개수를 제한할지 결정이 필요하다.

### 결정
**상품당 최대 10개**

### 근거
- S3 비용 관리
- 프론트엔드 로딩 성능 (이미지 많으면 로딩 느림)
- 일반적인 쇼핑몰 이미지 개수 범위 (3~7개)

### 구현
```java
long imageCount = imageRepository.countByProductId(productId);
if (imageCount >= 10) {
    throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
}
```

### ErrorCode 추가
```java
IMAGE_LIMIT_EXCEEDED(400, "상품당 이미지는 최대 10개까지 업로드할 수 있습니다")
```

---

## ADR-03-015: 이미지 파일 크기 제한

### 배경
업로드 가능한 이미지 파일 크기 제한이 필요하다.

### 결정
**최대 5MB** (설계 문서 명시)

### 근거
- 웹 최적화 이미지 크기로 충분
- S3 전송 비용 절감
- 모바일 환경 고려 (LTE/5G 환경에서도 빠른 로딩)

### 구현
**application.yaml:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB  # 여러 파일 업로드 대비
```

**S3Uploader 검증:**
```java
private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;  // 5MB

if (file.getSize() > MAX_FILE_SIZE) {
    throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
}
```

### ErrorCode 추가
```java
FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB를 초과할 수 없습니다")
```

---

## ADR-03-016: 이미지 파일 타입 검증

### 배경
허용할 이미지 파일 확장자를 정의해야 한다.

### 결정
**`.jpg`, `.jpeg`, `.png`, `.webp`** (설계 문서 명시)

### 근거
- WebP: 압축률 우수 (JPEG 대비 25~35% 작음), 최신 브라우저 지원
- GIF 제외: 애니메이션 불필요, 용량 큼

### 구현
```java
private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

String extension = getExtension(file.getOriginalFilename()).toLowerCase();
if (!ALLOWED_EXTENSIONS.contains(extension)) {
    throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
}
```

### MIME 타입 검증 (추가)
```java
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp"
);

if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
    throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
}
```

---

## ADR-03-017: 상품 수정 시 카테고리 변경

### 배경
상품 수정 시 카테고리 변경을 허용할지 결정이 필요하다.

### 결정
**허용 (제한 없음)**

### 근거
- 실무에서 카테고리 재분류 필요성 있음 (오분류 수정, 카테고리 개편)
- 데이터 무결성 문제 없음 (FK만 변경)
- 제약 조건 불필요

### 구현
```java
public void updateProduct(Long productId, ProductRequest request) {
    Product product = findProductById(productId);
    Category newCategory = findCategoryById(request.getCategoryId());

    product.update(newCategory, request.getName(), ...);  // 카테고리 변경 가능
}
```

---

## ADR-03-018: 상품 삭제 시 이미지 처리

### 배경
상품 삭제(INACTIVE) 시 연관된 이미지를 어떻게 처리할지 결정이 필요하다.

### 결정
**유지 (DB 레코드 + S3 파일 모두 유지)**

### 근거
- 소프트 삭제 정책과 일관성 (ADR-03-004)
- 복구 가능성 (INACTIVE → ACTIVE 전환 시 이미지 유지)
- 주문 이력에서 상품 이미지 참조 가능 (OrderItem → Product → ProductImage)

### 구현
```java
public void deleteProduct(Long productId) {
    Product product = findProductById(productId);
    product.deactivate();  // status = INACTIVE (이미지는 그대로)
}
```

### 대안
- 완전 삭제 시에만 S3 삭제: 관리자가 명시적으로 영구 삭제 시 처리
- 배치 작업: 일정 기간(예: 6개월) 후 INACTIVE 상품의 이미지 자동 삭제

---

## ADR-03-019: S3 업로드 실패 시 처리

### 배경
S3 업로드 실패 시 처리 방법이 필요하다.

### 결정
**즉시 예외 발생**

### 근거
- 명확한 오류 전달 (사용자에게 실패 이유 명시)
- 재시도는 AWS SDK 레벨에서 자동 처리 (기본 3회)
- fallback 이미지는 프론트엔드에서 처리

### 구현
```java
try {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(file.getContentType())
        .build();

    s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

} catch (S3Exception e) {
    throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
}
```

### ErrorCode (기존 사용)
```java
FILE_UPLOAD_FAILED(500, "파일 업로드에 실패했습니다")
```

---

## ADR-03-020: 로컬 개발 환경 S3 대체

### 배경
로컬 개발 환경에서 S3를 어떻게 대체할지 결정이 필요하다.

### 결정
**실제 S3 사용**

### 근거
- 설정 일관성 (로컬/운영 환경 동일)
- LocalStack 등 추가 의존성 불필요
- AWS Free Tier로 충분 (S3: 5GB 스토리지 + 20,000 GET 요청 무료)

### 구현
**application-local.yaml:**
```yaml
cloud:
  aws:
    credentials:
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
    region:
      static: ap-northeast-2
    s3:
      bucket: ${AWS_S3_BUCKET}
```

**application-secret.yaml (로컬):**
```yaml
AWS_ACCESS_KEY: "AKIA..."
AWS_SECRET_KEY: "xxxx..."
AWS_S3_BUCKET: "shopper-dev"
```

### 대안
- LocalStack: Docker 설정 필요, 개발 환경 복잡도 증가
- 로컬 파일 시스템: 운영과 다른 동작, 배포 시 문제 발생 가능

---

## 구현 요약

### S3Uploader 구현 사항
```java
public class S3Uploader {
    - uploadImage(MultipartFile file): String  // S3 URL 반환
    - deleteImage(String url): void
    - validateFile(MultipartFile file): void   // 크기, 타입 검증
    - generateFileName(String originalFilename): String  // UUID + 확장자
    - generateS3Key(String fileName): String   // 날짜별 경로
}
```

### 관리자 API 엔드포인트
```
POST   /api/admin/products              — 상품 등록
PUT    /api/admin/products/{id}         — 상품 수정
DELETE /api/admin/products/{id}         — 상품 삭제 (INACTIVE)
POST   /api/admin/products/{id}/images  — 이미지 업로드
DELETE /api/admin/products/{id}/images/{imageId}  — 이미지 삭제
```

### ErrorCode 추가 필요
```java
IMAGE_LIMIT_EXCEEDED(400, "상품당 이미지는 최대 10개까지 업로드할 수 있습니다")
FILE_SIZE_EXCEEDED(400, "파일 크기는 5MB를 초과할 수 없습니다")
PRODUCT_IMAGE_NOT_FOUND(404, "상품 이미지를 찾을 수 없습니다")
```

### application.yaml 추가 설정
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
```

---

## 구현 순서

1. **application.yaml 업데이트** — multipart 설정, S3 설정 확인
2. **ErrorCode 추가** — IMAGE_LIMIT_EXCEEDED, FILE_SIZE_EXCEEDED, PRODUCT_IMAGE_NOT_FOUND
3. **S3Uploader 작성** — infra/s3/S3Uploader.java
4. **ProductService 확장** — 관리자 CRUD 메서드 추가
5. **AdminProductController 작성** — 관리자 전용 컨트롤러
6. **이미지 업로드 API** — POST /admin/products/{id}/images

---

**문서 버전:** 1.0
**이전 문서:** ADR-003-02 (엔티티 설계)
**다음 단계:** 구현 완료 후 Phase 4 (장바구니/주문) 진행
