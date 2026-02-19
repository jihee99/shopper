# ADR-01-001 ~ ADR-01-007: Phase 1 프로젝트 세팅 아키텍처 결정

> **Phase**: 1 - 프로젝트 세팅
> **작성일**: 2026-02-19
> **관련 계획**: `00_프로젝트설계계획.md` Phase 1

---

## 목차

| ADR                                      | 주제 | 상태 |
|------------------------------------------|------|------|
| [ADR-01-001](#adr-01-001-spring-boot-버전)  | Spring Boot 버전 (4.0.2 vs 3.x) | ✅ 결정됨 |
| [ADR-01-002](#adr-01-002-jwt-라이브러리-선택)      | JWT 라이브러리 선택 | ✅ 결정됨 |
| [ADR-01-003](#adr-01-003-querydsl-구성-방식)    | QueryDSL 구성 방식 | ✅ 결정됨 |
| [ADR-01-004](#adr-01-004-환경-설정-및-시크릿-관리-전략) | 환경 설정 및 시크릿 관리 전략 | ✅ 결정됨 |
| [ADR-01-005](#adr-01-005-공통-api-응답-포맷)      | 공통 API 응답 포맷 | ✅ 결정됨 |
| [ADR-01-006](#adr-01-006-에러-코드-관리-전략)       | 에러 코드 관리 전략 | ✅ 결정됨 |
| [ADR-01-007](#adr-01-007-패키지-구조-전략)         | 패키지 구조 전략 | ✅ 결정됨 |

---

## ADR-01-001: Spring Boot 버전

### 상태
결정됨

### 배경 및 문제

`00_프로젝트설계.md`는 **Spring Boot 3.x** 기반을 명시하고 있으나,
프로젝트 초기화 시 **Spring Boot 4.0.2**가 사용되었다. 두 버전 중 하나로 통일해야 한다.

**Spring Boot 4.x의 주요 변경사항:**
- Spring Framework 7.0 기반
- Jakarta EE 11 필요 (Java SE 11 API 기준 최신)
- Java 17 최소 요구 (현재 프로젝트와 동일)
- 일부 서드파티 라이브러리(QueryDSL 등)의 4.x 공식 지원 지연 가능성

**고려 옵션:**

| 옵션 | 장점 | 단점 |
|------|------|------|
| **4.0.2 유지** | 최신 기능, 장기 지원 예상, 이미 초기화 완료 | 라이브러리 호환성 검증 필요 |
| **3.3.x 다운그레이드** | 풍부한 레퍼런스, 검증된 라이브러리 호환성 | 이미 초기화된 프로젝트를 되돌려야 함 |

### 결정

**Spring Boot 4.0.2를 그대로 유지한다.**

### 근거

1. 프로젝트가 이미 4.0.2로 초기화되어 있어 다운그레이드 비용이 더 크다.
2. 이 프로젝트는 포트폴리오 목적이므로 최신 버전 경험이 가치 있다.
3. 핵심 의존성(Spring Security, Spring Data JPA, jjwt)은 Jakarta EE 11과 호환된다.
4. QueryDSL 호환성은 ADR-003에서 별도로 해결한다.

### 영향

- `build.gradle`의 Spring Boot 버전은 `4.0.2`로 고정
- 사용 라이브러리는 Jakarta EE 11 호환 버전을 명시적으로 지정
- 설계 문서(`00_프로젝트설계.md`)의 "Spring Boot 3.x" 표기는 "4.x"로 업데이트 필요

---

## ADR-01-002: JWT 라이브러리 선택

### 상태
결정됨

### 배경 및 문제

JWT Access Token / Refresh Token 생성 및 검증을 위한 라이브러리를 선택해야 한다.

**후보 라이브러리:**

| 라이브러리 | 버전 | 특징 |
|-----------|------|------|
| **jjwt** (io.jsonwebtoken) | 0.12.x | 국내 레퍼런스 최다, 간결한 Builder API, Spring Boot와 무관하게 동작 |
| **nimbus-jose-jwt** | 9.x | RFC 7519 완벽 준수, 엔터프라이즈 표준, Spring Security OAuth2와 통합 용이 |
| **spring-security-oauth2-jose** | Spring Boot 내장 | Spring 공식, 추가 의존성 없음, Resource Server 구성에 최적화 |

### 결정

**jjwt 0.12.x (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)을 사용한다.**

```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

### 근거

1. **레퍼런스**: 국내 Spring Boot JWT 구현 사례의 대다수가 jjwt를 사용해 디버깅이 용이하다.
2. **API 단순성**: `Jwts.builder()` / `Jwts.parser()` 체이닝 방식으로 직관적이다.
3. **Spring 비종속**: Spring Security 내부 동작과 분리되어 테스트가 쉽다.
4. **0.12.x 변경**: 0.11.x 대비 서명 알고리즘 API가 타입 안전하게 개선되었다 (`Jwts.SIG.HS256` 방식).

### 영향

- `JwtProvider.java`에서 토큰 생성/검증/클레임 파싱을 모두 캡슐화
- 알고리즘: `HS256` (HMAC-SHA256), Secret Key는 환경 변수로 관리 (ADR-004 참조)
- Access Token 만료: 15분 / Refresh Token 만료: 7일

---

## ADR-01-003: QueryDSL 구성 방식

### 상태
결정됨

### 배경 및 문제

상품 목록 조회(페이징 + 필터 + 검색)처럼 동적 쿼리가 필요한 경우 QueryDSL을 사용한다.
Spring Boot 4.x / Jakarta EE 11 환경에서 QueryDSL 설정에 두 가지 선택지가 있다.

**고려 옵션:**

| 옵션 | 방식 | 현황 |
|------|------|------|
| **QueryDSL 5.1.x** | APT 플러그인 또는 `annotationProcessor`로 Q클래스 생성 | Jakarta EE 지원 (`jakarta.persistence` 패키지 사용) |
| **Spring Data JPA Specification** | `JpaSpecificationExecutor` 인터페이스 활용 | QueryDSL 없이 동적 쿼리 가능, 복잡한 조인에 불리 |

**QueryDSL 5.1.x Gradle 설정 방식 비교:**

| 방식 | 특징 |
|------|------|
| `com.ewerk.gradle.plugins.querydsl` 플러그인 | 구버전, Spring Boot 4 미지원 가능성 |
| `annotationProcessor` 직접 등록 | 플러그인 없이 순수 Gradle AP 방식, Spring Boot 4와 호환 |

### 결정

**QueryDSL 5.1.0을 `annotationProcessor` 방식으로 구성한다.**

```gradle
implementation 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
annotationProcessor 'com.querydsl:querydsl-apt:5.1.0:jakarta'
annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
```

Q클래스 생성 경로: `src/main/generated`

```gradle
sourceSets {
    main.java.srcDirs += 'src/main/generated'
}

clean {
    delete file('src/main/generated')
}
```

### 근거

1. **Jakarta 호환**: `:jakarta` classifier를 사용하면 `javax.persistence` 대신 `jakarta.persistence`를 사용하여 Spring Boot 4와 호환된다.
2. **플러그인 불필요**: 외부 Gradle 플러그인 없이 표준 `annotationProcessor`만으로 구성해 의존성 복잡도를 줄인다.
3. **IDE 지원**: IntelliJ는 `src/main/generated`를 자동으로 소스 루트로 인식한다.

### 영향

- `src/main/generated/` 디렉토리는 `.gitignore`에 추가 (자동 생성 파일이므로)
- 엔티티 변경 시 `./gradlew compileJava`를 실행해 Q클래스를 재생성해야 함
- `ProductRepositoryCustom` + `ProductRepositoryImpl` 패턴으로 복잡 쿼리 분리

---

## ADR-01-004: 환경 설정 및 시크릿 관리 전략

### 상태
결정됨

### 배경 및 문제

DB 비밀번호, JWT Secret Key, OAuth2 Client Secret, AWS 자격증명 등 민감 정보를
어떻게 관리하고 환경별(로컬/운영)로 분리할지 결정해야 한다.

**고려 옵션:**

| 옵션 | 장점 | 단점 |
|------|------|------|
| 환경 변수 직접 주입 | 가장 안전, 12-Factor 표준 | 로컬 개발 설정이 번거로움 |
| `application-secret.yml` (.gitignore) | 로컬 개발 편의, 프로파일 분리 용이 | 파일 분실 시 복구 어려움 |
| Spring Cloud Config | 중앙 집중 관리, 운영 편의 | 인프라 추가 필요, 소규모 프로젝트에 과함 |

### 결정

**프로파일 분리 + `application-secret.yml` 방식을 채택한다.**

**파일 구조:**

```
src/main/resources/
├── application.yaml            # 공통 설정 (프로파일 무관)
├── application-local.yaml      # 로컬 개발 전용 (DB, Redis 로컬 주소)
├── application-prod.yaml       # 운영 환경 (환경 변수 참조)
└── application-secret.yaml     # 민감 정보 (Git 제외 - .gitignore 등록)
```

**`application-secret.yaml` 내용 예시 (Git 제외):**

```yaml
# .gitignore에 추가: application-secret.yaml
jwt:
  secret: ${JWT_SECRET}     # 로컬: 직접 값 입력, 운영: 환경변수

spring:
  datasource:
    password: ${DB_PASSWORD}
  data:
    redis:
      password: ${REDIS_PASSWORD}

oauth2:
  google:
    client-secret: ${GOOGLE_CLIENT_SECRET}
  kakao:
    client-secret: ${KAKAO_CLIENT_SECRET}

cloud:
  aws:
    credentials:
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
```

**프로파일 활성화:**
- 로컬: `spring.profiles.active=local,secret`
- 운영: `spring.profiles.active=prod` (환경 변수로 시크릿 주입)

### 근거

1. **보안**: 민감 정보가 Git에 커밋되지 않는다.
2. **로컬 편의**: `application-secret.yaml`에 개발용 값을 직접 입력해 환경 변수 설정 부담이 없다.
3. **운영 분리**: `application-prod.yaml`은 `${환경변수}` 플레이스홀더만 포함하여 배포 환경에서 환경 변수로 주입한다.

### 영향

- `.gitignore`에 `application-secret.yaml` 반드시 추가
- 신규 개발자는 `application-secret.yaml.example` (빈 값) 파일을 참고해 직접 생성
- 운영 배포 시 Docker/EC2 환경 변수 또는 AWS Secrets Manager로 전환 가능

---

## ADR-01-005: 공통 API 응답 포맷

### 상태
결정됨

### 배경 및 문제

모든 API가 일관된 응답 구조를 반환하도록 `ApiResponse<T>` 래퍼 클래스를 설계해야 한다.
프론트엔드(React)에서 응답을 파싱하는 방식에 직접적인 영향을 준다.

**후보 구조:**

```
# 옵션 A: HTTP 상태 코드 + 단순 메시지
{ "message": "success", "data": {...} }

# 옵션 B: 커스텀 코드 포함
{ "code": "SUCCESS", "message": "요청이 성공했습니다", "data": {...} }

# 옵션 C: 타임스탬프 포함 (상세)
{ "success": true, "code": "SUCCESS", "message": "...", "data": {...}, "timestamp": "..." }
```

### 결정

**옵션 B 방식을 채택한다. 성공/실패 공통 래퍼 구조:**

```java
// 성공 응답
ApiResponse<T> {
    boolean success;   // true
    String  message;   // "요청이 성공했습니다"
    T       data;      // 실제 데이터 (null 가능)
}

// 실패 응답 (GlobalExceptionHandler 반환)
ApiResponse<Void> {
    boolean success;   // false
    String  code;      // "USER_NOT_FOUND" (ErrorCode enum name)
    String  message;   // "사용자를 찾을 수 없습니다"
    Void    data;      // null
}
```

**HTTP 상태 코드와 병행 사용:**
- 성공: HTTP 200/201 + `success: true`
- 실패: HTTP 400/401/403/404/500 + `success: false` + `code`

### 근거

1. **HTTP 표준 준수**: HTTP 상태 코드를 제거하지 않아 REST 규약을 유지한다.
2. **에러 식별**: `code` 필드로 프론트엔드가 에러 종류를 구분해 UI 처리가 가능하다.
3. **단순성**: 타임스탬프 등 불필요한 필드를 제거해 응답 크기를 줄인다.
4. **일관성**: 성공/실패 모두 동일한 래퍼 클래스를 사용해 파싱 로직이 통일된다.

### 영향

```java
// 사용 예시
return ResponseEntity.ok(ApiResponse.success("상품 조회 성공", productDto));
return ResponseEntity.status(404).body(ApiResponse.failure(ErrorCode.PRODUCT_NOT_FOUND));
```

- `ApiResponse.java`에 정적 팩토리 메서드 `success()`, `failure()` 구현
- 프론트엔드는 `response.data.success`로 성공 여부 먼저 확인 후 `data` 파싱

---

## ADR-01-006: 에러 코드 관리 전략

### 상태
결정됨

### 배경 및 문제

애플리케이션 전반의 에러 코드를 어떻게 정의하고 관리할지 결정해야 한다.
에러 코드는 프론트엔드와의 계약이므로 일관성과 확장성이 중요하다.

**고려 옵션:**

| 옵션 | 장점 | 단점 |
|------|------|------|
| **단일 `ErrorCode` enum** | 전체 에러를 한 곳에서 파악 가능 | enum이 비대해질 수 있음 |
| **도메인별 분리** (`UserErrorCode`, `ProductErrorCode` 등) | 도메인 응집도 높음 | 에러 코드 일괄 파악이 어려움 |
| **숫자 코드** (`1001`, `2001` 등) | 간결 | 의미 파악 어려움, 번호 관리 비용 |

### 결정

**단일 `ErrorCode` enum에 도메인별 그룹으로 정의한다.**

```java
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────
    INVALID_INPUT(400, "잘못된 입력값입니다"),
    UNAUTHORIZED(401, "인증이 필요합니다"),
    FORBIDDEN(403, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다"),

    // ── 인증/회원 ──────────────────────────────────────
    EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다"),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
    INVALID_PASSWORD(401, "비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다"),

    // ── 상품 ───────────────────────────────────────────
    PRODUCT_NOT_FOUND(404, "상품을 찾을 수 없습니다"),
    OUT_OF_STOCK(409, "재고가 부족합니다"),

    // ── 주문/결제 ──────────────────────────────────────
    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다"),
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액이 일치하지 않습니다"),
    PAYMENT_ALREADY_COMPLETED(409, "이미 완료된 결제입니다");

    private final int httpStatus;
    private final String message;
}
```

### 근거

1. **가시성**: 전체 에러 코드를 단일 파일에서 파악할 수 있어 중복 정의를 방지한다.
2. **HTTP 상태 내포**: 각 ErrorCode가 적절한 HTTP 상태 코드를 포함해 `GlobalExceptionHandler`가 자동으로 응답 상태를 결정한다.
3. **프론트엔드 계약**: 문자열 코드(`USER_NOT_FOUND`)는 숫자 코드보다 의미가 명확하다.
4. **확장 용이**: 새 도메인 추가 시 그룹 주석 아래에 추가하면 된다.

### 영향

- `GlobalExceptionHandler`는 `CustomException`에서 `ErrorCode`를 꺼내 HTTP 상태와 메시지를 결정
- Phase별로 새 에러 코드를 해당 그룹에 추가

---

## ADR-01-007: 패키지 구조 전략

### 상태
결정됨

### 배경 및 문제

백엔드 코드를 **레이어 기준**(controller / service / repository)으로 나눌지,
**도메인 기준**(user / product / order)으로 나눌지 결정해야 한다.

**고려 옵션:**

| 옵션 | 구조 예시 | 특징 |
|------|-----------|------|
| **레이어 기준** | `controller/UserController` `service/UserService` | 전통적, 소규모에 단순 |
| **도메인 기준** | `domain/user/UserController` `domain/user/UserService` | 높은 응집도, 도메인 독립성 |
| **도메인 + 패키지 by Feature** | `domain/user/{controller,service,repository,entity,dto}` | 기능별 완전 캡슐화 |

### 결정

**도메인 기준 패키지 구조를 채택하되, 도메인 내에서 역할별 하위 패키지를 사용한다.**

```
com.jihee.shopper/
├── domain/
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── SignupRequest.java
│   │       └── TokenResponse.java
│   ├── user/
│   │   ├── UserController.java
│   │   ├── UserService.java
│   │   ├── UserRepository.java
│   │   └── entity/
│   │       ├── User.java
│   │       ├── SocialAccount.java
│   │       └── Address.java
│   ├── product/
│   │   ├── ProductController.java
│   │   ├── ProductService.java
│   │   ├── ProductRepository.java
│   │   ├── ProductRepositoryCustom.java   ← QueryDSL 인터페이스
│   │   ├── ProductRepositoryImpl.java     ← QueryDSL 구현
│   │   └── entity/
│   │       ├── Product.java
│   │       ├── Category.java
│   │       └── ProductImage.java
│   ├── cart/  ...
│   ├── order/ ...
│   └── payment/ ...
├── global/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── QuerydslConfig.java
│   │   ├── RedisConfig.java
│   │   └── CorsConfig.java
│   ├── security/
│   │   ├── JwtProvider.java
│   │   ├── JwtFilter.java
│   │   ├── CustomUserDetails.java
│   │   ├── CustomUserDetailsService.java
│   │   └── oauth2/
│   │       ├── CustomOAuth2UserService.java
│   │       └── OAuth2SuccessHandler.java
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── CustomException.java
│   │   └── GlobalExceptionHandler.java
│   └── common/
│       ├── BaseEntity.java
│       ├── ApiResponse.java
│       └── PageResponse.java
└── infra/
    └── s3/
        └── S3Uploader.java
```

### 근거

1. **응집도**: 같은 도메인의 Controller / Service / Repository / Entity / DTO가 한 패키지에 모여 있어 도메인 변경 시 영향 범위가 명확하다.
2. **설계 문서 일치**: `00_프로젝트설계.md`의 7.1 Backend 구조와 동일하여 문서와 코드의 일관성을 유지한다.
3. **global 분리**: 보안, 설정, 예외 처리 등 횡단 관심사는 `global` 패키지로 분리해 도메인과 구분한다.
4. **infra 분리**: AWS S3 등 외부 인프라 연동은 `infra` 패키지로 분리해 교체 가능성을 열어둔다.

### 영향

- 도메인 간 의존성은 단방향으로 유지 (예: `order` → `product` O, `product` → `order` X)
- 공유 DTO가 필요한 경우 `global/common/dto/` 또는 해당 도메인의 dto에 정의하고 참조

---

## 결정 요약

| ADR | 결정 내용 |
|-----|-----------|
| ADR-01-001 | Spring Boot **4.0.2** 유지 |
| ADR-01-002 | JWT 라이브러리: **jjwt 0.12.6** |
| ADR-01-003 | QueryDSL: **5.1.0 + `annotationProcessor` (`:jakarta` classifier)** |
| ADR-01-004 | 설정: **프로파일 분리 + `application-secret.yaml` (.gitignore)** |
| ADR-01-005 | 응답 포맷: **`ApiResponse<T>` { success, message, code, data }** |
| ADR-01-006 | 에러 코드: **단일 `ErrorCode` enum, 도메인별 그룹 주석** |
| ADR-01-007 | 패키지: **도메인 기준 (`domain/` + `global/` + `infra/`)** |

---

*본 문서는 구현 진행에 따라 업데이트됩니다 | Last Updated: 2026-02-19*
