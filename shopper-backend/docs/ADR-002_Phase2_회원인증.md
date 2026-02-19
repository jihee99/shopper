# ADR-02-001 ~ ADR-02-007: Phase 2 회원/인증 아키텍처 결정

> **Phase**: 2 - 회원/인증
> **작성일**: 2026-02-19
> **관련 계획**: `00_프로젝트설계계획.md` Phase 2

---

## 목차

| ADR | 주제 | 상태 |
|-----|------|------|
| [ADR-02-001](#adr-02-001-jwt-전송-방식) | JWT 전송 방식 | ✅ 결정됨 |
| [ADR-02-002](#adr-02-002-refresh-token-전달-및-redis-저장-구조) | Refresh Token 전달 및 Redis 저장 구조 | ✅ 결정됨 |
| [ADR-02-003](#adr-02-003-refresh-token-rotation-전략) | Refresh Token Rotation 전략 | ✅ 결정됨 |
| [ADR-02-004](#adr-02-004-비밀번호-해시-알고리즘) | 비밀번호 해시 알고리즘 | ✅ 결정됨 |
| [ADR-02-005](#adr-02-005-jwt-payload-클레임-구성) | JWT Payload 클레임 구성 | ✅ 결정됨 |
| [ADR-02-006](#adr-02-006-oauth2-이메일-중복-처리-전략) | OAuth2 이메일 중복 처리 전략 | ✅ 결정됨 |
| [ADR-02-007](#adr-02-007-oauth2-성공-후-jwt-전달-방식) | OAuth2 성공 후 JWT 전달 방식 | ✅ 결정됨 |

---

## ADR-02-001: JWT 전송 방식

### 상태
결정됨

### 배경 및 문제

클라이언트(React SPA)가 서버에 인증이 필요한 요청을 보낼 때 JWT Access Token을
어떤 방식으로 전달할지 결정해야 한다.

**고려 옵션:**

| 방식 | 장점 | 단점 |
|------|------|------|
| **Authorization Header** (`Bearer {token}`) | SPA 표준, CORS 설정 단순, 구현 용이, 레퍼런스 최다 | XSS 공격 시 localStorage 탈취 위험 |
| **HttpOnly Cookie** | XSS 공격으로부터 토큰 보호 | CSRF 방어 로직 추가 필요, CORS 쿠키 설정 복잡 |

### 결정

**Authorization Header (Bearer Token) 방식을 사용한다.**

**전체 흐름:**
```
1. POST /api/auth/login
   ← Response Body: { accessToken, refreshToken }

2. 프론트엔드(Zustand)가 accessToken을 메모리에 저장

3. 모든 API 요청 시 Axios 인터셉터가 자동으로 헤더 주입
   → Authorization: Bearer {accessToken}

4. 401 응답 시 Axios 인터셉터가 자동으로 토큰 재발급 처리
```

### 근거

1. **SPA 표준**: React + Axios 환경에서 Authorization 헤더 방식이 압도적으로 많이 사용된다.
2. **구현 단순성**: Axios 인터셉터 하나로 모든 요청에 자동 적용된다.
3. **CORS 단순화**: 쿠키 방식 대비 `withCredentials`, `SameSite` 등 추가 설정이 불필요하다.
4. **포트폴리오 적합**: XSS 위험은 프론트엔드에서 CSP(Content Security Policy) 적용으로 경감 가능하다.

### 영향

- `JwtFilter.java`: `Authorization` 헤더에서 `Bearer ` 접두어를 제거해 토큰 추출
- 프론트엔드 `axios.ts`: 요청 인터셉터에서 `Authorization: Bearer {token}` 자동 주입
- Access Token은 만료 시간이 짧으므로(15분) 탈취 피해 범위가 제한됨

---

## ADR-02-002: Refresh Token 전달 및 Redis 저장 구조

### 상태
결정됨

### 배경 및 문제

Refresh Token을 클라이언트에 어떻게 전달할지, 그리고 서버(Redis)에 어떤 구조로
저장할지 결정해야 한다.

**전달 방식 옵션:**

| 방식 | 장점 | 단점 |
|------|------|------|
| **Response Body 포함** | 구현 단순, 프론트에서 제어 용이 | JS 접근 가능 → XSS 위험 |
| **HttpOnly Cookie** | XSS로부터 토큰 보호 | CSRF 방어 필요, CORS 쿠키 설정 복잡 |

**Redis Key 구조 옵션:**

| Key 구조 | 특징 |
|----------|------|
| `RT:{userId}` | 사용자당 1개 세션, 구현 단순, 새 로그인 시 기존 토큰 자동 무효화 |
| `RT:{userId}:{deviceId}` | 멀티 디바이스 지원, 구현 복잡 |
| `RT:{tokenHash}` | 토큰 자체가 Key, 탈취 토큰 개별 무효화 가능, 조회 구조 복잡 |

### 결정

**Response Body로 전달 + Redis Key `RT:{userId}` 구조를 사용한다.**

**Redis 저장 구조:**
```
Key   : "RT:{userId}"          예) "RT:1"
Value : {refreshToken 문자열}
TTL   : 604800초 (7일)
```

**로그인 시 동작:**
```
1. 기존 "RT:{userId}" 키가 있으면 덮어쓰기 (이전 세션 자동 무효화)
2. 새 Refresh Token을 "RT:{userId}"에 저장 (TTL 7일)
3. Response Body에 { accessToken, refreshToken } 반환
```

### 근거

1. **단순성**: `RT:{userId}` 구조는 구현이 간단하고 직관적이다.
2. **단일 세션 보장**: 새 디바이스에서 로그인 시 기존 세션이 자동 무효화되어 보안이 강화된다.
3. **포트폴리오 범위**: 멀티 디바이스 지원은 서비스 요구사항이 아니므로 복잡도를 추가할 필요가 없다.
4. **Access Token 보완**: Access Token이 15분으로 짧아 Refresh Token이 탈취되더라도 피해 시간이 RTR로 제한된다.

### 영향

- `RedisConfig.java`: `StringRedisTemplate` Bean 구성
- `AuthService.java`: 로그인 시 `RT:{userId}` 저장, 로그아웃 시 삭제
- `TokenResponse.java` DTO: `accessToken`, `refreshToken` 두 필드 포함
- 프론트엔드: `refreshToken`을 `localStorage`에 저장, 재발급 시 사용

---

## ADR-02-003: Refresh Token Rotation 전략

### 상태
결정됨

### 배경 및 문제

Refresh Token 재발급 시 기존 토큰을 어떻게 처리할지 결정해야 한다.
Refresh Token이 탈취된 경우에도 피해를 최소화하는 전략이 필요하다.

**고려 옵션:**

| 전략 | 동작 | 보안성 |
|------|------|--------|
| **RTR 완전 교체** | 재발급 시 기존 토큰 즉시 삭제 + 새 토큰 발급 | 높음: 탈취 토큰 재사용 탐지 가능 |
| **슬라이딩 만료** | 사용할 때마다 TTL 연장 | 낮음: 활성 사용자는 영구 세션 위험 |
| **고정 만료** | Rotation 없이 7일 후 무조건 만료 | 낮음: 탈취 대응 불가 |

### 결정

**RTR(Refresh Token Rotation) 완전 교체 방식을 사용한다.**

**`POST /api/auth/refresh` 처리 흐름:**
```
1. 요청: { refreshToken: "기존토큰" }

2. Redis에서 "RT:{userId}" 조회
   → 저장된 토큰 vs 요청 토큰 일치 여부 확인
   → 불일치: 401 INVALID_TOKEN (탈취 의심 → 해당 유저 세션 전체 강제 종료)

3. 일치:
   → 기존 Refresh Token Redis에서 즉시 삭제
   → 새 Access Token (15분) 생성
   → 새 Refresh Token (7일) 생성
   → Redis "RT:{userId}"에 새 Refresh Token 저장
   → Response: { accessToken: 새토큰, refreshToken: 새토큰 }
```

**탈취 시나리오 대응:**
```
정상 사용자 A: refreshToken_v1 보유
공격자      B: refreshToken_v1 탈취

공격자 B가 먼저 재발급 요청
  → Redis: RT:1 = refreshToken_v2 (갱신됨)
  → B는 refreshToken_v2 획득

이후 정상 사용자 A가 재발급 요청
  → Redis의 refreshToken_v2 ≠ A가 가진 refreshToken_v1
  → 401 응답 + Redis RT:1 즉시 삭제 (전체 세션 종료)
  → A는 재로그인 필요 → 이 시점에 탈취 사실 인지
```

### 근거

1. **탈취 탐지**: 이미 사용된 토큰으로 재발급 시도 시 불일치를 감지해 세션을 강제 종료할 수 있다.
2. **표준 패턴**: RFC 6749에서 권장하는 Refresh Token Rotation과 동일한 방식이다.
3. **Redis 적합**: Redis의 O(1) 읽기/쓰기/삭제가 RTR의 원자적 처리에 최적이다.

### 영향

- `AuthService.java`: `refresh()` 메서드에서 토큰 일치 확인 → 삭제 → 재발급 순서 보장
- Race Condition 주의: 동시 재발급 요청 시 첫 번째 요청만 성공, 나머지 401 처리 (단일 세션 구조에서 허용 가능)
- `ErrorCode.java`: `INVALID_TOKEN` (이미 정의됨) 사용

---

## ADR-02-004: 비밀번호 해시 알고리즘

### 상태
결정됨

### 배경 및 문제

회원가입 시 사용자 비밀번호를 DB에 저장하기 전에 단방향 해시 처리가 필요하다.

**고려 옵션:**

| 알고리즘 | 보안 강도 | 특징 |
|---------|----------|------|
| **BCrypt** | ★★★★ | Spring Security 기본, 국내외 레퍼런스 최다, 충분한 보안성 |
| **Argon2** | ★★★★★ | 메모리 집약적, OWASP 최우선 권장, 설정 복잡 |
| **SCrypt** | ★★★★ | 메모리 기반, 레퍼런스 드뭄 |

### 결정

**BCrypt (strength 10)를 사용한다.**

```java
// SecurityConfig.java 또는 별도 Bean
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // 기본 strength: 10
}
```

**동작 방식:**
```
회원가입: plainPassword → BCrypt 해싱 → DB 저장
로그인  : plainPassword + DB 해시 → passwordEncoder.matches() → 일치 여부 확인
```

### 근거

1. **Spring Security 표준**: `PasswordEncoder` 인터페이스의 기본 구현체로 추가 설정이 불필요하다.
2. **충분한 보안성**: strength 10은 일반적인 서비스에서 충분한 보안 수준이며, 국내 대부분의 서비스가 사용한다.
3. **단순성**: BCrypt는 Salt를 자체적으로 생성·포함하므로 별도의 Salt 관리가 불필요하다.
4. **업그레이드 가능**: Spring Security의 `DelegatingPasswordEncoder`를 통해 추후 알고리즘 변경이 가능하다.

### 영향

- `SecurityConfig.java`: `PasswordEncoder` Bean 등록
- `AuthService.java`: `passwordEncoder.encode()`, `passwordEncoder.matches()` 사용
- DB `users.password` 컬럼: BCrypt 해시 문자열 저장 (최대 60자)

---

## ADR-02-005: JWT Payload 클레임 구성

### 상태
결정됨

### 배경 및 문제

Access Token의 Payload에 어떤 정보를 담을지 결정해야 한다.
클레임이 많을수록 DB 조회가 줄지만 토큰 크기가 커지고, 정보 변경 시 즉시 반영이 불가능하다.

**Subject(sub) 기준:**

| 선택 | 특징 |
|------|------|
| `userId` (Long) | 불변값, 식별자로 적합, 변경 불가 |
| `email` (String) | 가독성 좋음, 이메일 변경 시 불일치 위험 |

**클레임 구성 옵션:**

| 옵션 | 포함 클레임 | 특징 |
|------|------------|------|
| 최소 | `sub`, `role` | 토큰 작음, 대부분 요청에서 DB 추가 조회 필요 |
| **표준** | `sub`, `email`, `role` | 균형 잡힌 구성, 대부분 요청에서 DB 조회 불필요 |
| 풀 | `sub`, `email`, `name`, `role` | DB 조회 최소화, 회원정보 변경 반영 지연 (15분) |

### 결정

**표준 구성 (`sub: userId`, `email`, `role`)을 사용한다.**

**Access Token Payload:**
```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "ROLE_USER",
  "iat": 1700000000,
  "exp": 1700000900
}
```

**`JwtProvider.java` 생성 예시:**
```java
String accessToken = Jwts.builder()
    .subject(String.valueOf(userId))
    .claim("email", email)
    .claim("role", role.name())
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
    .signWith(secretKey, Jwts.SIG.HS256)
    .compact();
```

**`CustomUserDetails` 복원 (JwtFilter에서):**
```java
Long userId = Long.parseLong(claims.getSubject());
String role = claims.get("role", String.class);
// DB 조회 없이 인증 객체 생성
```

### 근거

1. **`sub`로 `userId` 사용**: email은 변경 가능한 값이므로 불변값인 userId를 식별자로 사용한다.
2. **`email` 포함**: 회원 정보 조회 API, 응답 생성 등 대부분의 요청에서 DB 조회 없이 처리 가능하다.
3. **`role` 포함**: 권한 체크를 DB 조회 없이 토큰만으로 처리해 Spring Security Filter에서 인가가 가능하다.
4. **`name` 제외**: name은 변경 가능성이 있고, 필요 시 별도 `/api/users/me` API로 조회하면 충분하다.

### 영향

- `JwtProvider.java`: 토큰 생성 시 `email`, `role` 클레임 포함
- `JwtFilter.java`: 토큰 파싱 후 `userId`, `role`로 `CustomUserDetails` 생성 (DB 조회 없음)
- `CustomUserDetails.java`: `userId`, `email`, `role` 필드 보유
- email 또는 role 변경 시 기존 Access Token은 만료(15분)될 때까지 구 정보를 가짐 → 허용 가능 수준

---

## ADR-02-006: OAuth2 이메일 중복 처리 전략

### 상태
결정됨

### 배경 및 문제

OAuth2 소셜 로그인 시 두 가지 엣지 케이스를 처리해야 한다.

1. **이메일 중복**: 소셜 계정의 이메일이 기존 일반(이메일+비밀번호) 계정의 이메일과 동일한 경우
2. **이메일 미제공**: Kakao는 이메일 제공이 선택사항 → 이메일 없이 가입된 경우

**이메일 중복 처리 옵션:**

| 전략 | 동작 | 특징 |
|------|------|------|
| **에러 반환** | "이미 해당 이메일로 가입된 계정이 있습니다" | 안전, 사용자가 명시적으로 연동 선택 |
| 자동 연동 | `SocialAccount`를 기존 `User`에 추가 | UX 편의, 이메일 소유 미검증 보안 위험 |
| 별도 계정 | 소셜/일반 계정 완전 분리 | 동일 사용자가 두 계정 보유 문제 |

**Kakao 이메일 미제공 옵션:**

| 전략 | 동작 |
|------|------|
| **임시 이메일 생성** | `{provider}_{providerId}@social.shopper` 형식으로 내부 식별용 이메일 생성 |
| 가입 거부 | 이메일 없으면 소셜 로그인 불가 처리 |
| null 허용 | DB email 컬럼 nullable, 별도 식별자 사용 |

### 결정

**이메일 중복: 에러 반환 / Kakao 이메일 미제공: 임시 이메일 자동 생성**

**소셜 로그인 처리 흐름 (`CustomOAuth2UserService`):**
```
1. 소셜 Provider에서 사용자 정보 수신

2. email 존재 여부 확인
   - email 없음(Kakao 미제공):
     → "{provider}_{providerId}@social.shopper" 임시 이메일 생성

3. social_accounts 테이블에서 (provider, providerId) 조회
   - 기존 소셜 계정 있음 → 로그인 처리 (기존 User 반환)

4. 신규 소셜 계정인 경우:
   - users 테이블에서 해당 email 조회
     - 동일 email의 일반 계정 존재 → OAuth2AuthenticationException
       ("이미 해당 이메일로 가입된 일반 계정이 있습니다. 일반 로그인을 이용해주세요.")
     - 없음 → 신규 User + SocialAccount 생성 → 로그인 처리
```

### 근거

1. **보안**: 자동 연동은 소셜 Provider의 이메일 인증만으로 기존 계정에 접근을 허용하므로 보안 위험이 있다.
2. **명확성**: 에러 메시지로 사용자가 상황을 인지하고 적절한 행동(일반 로그인 사용)을 취할 수 있다.
3. **임시 이메일**: Kakao 이메일 미제공 시에도 DB 스키마 변경 없이 일관된 처리가 가능하다.

### 영향

- `CustomOAuth2UserService.java`: 위 흐름대로 구현
- `SocialAccount.java` 엔티티: `provider`(GOOGLE/KAKAO), `providerId` 필드
- `ErrorCode.java`: `EMAIL_ALREADY_EXISTS` (이미 정의됨) 사용
- 추후 개선: 로그인 화면에서 "소셜 계정 연동" 기능 추가 가능

---

## ADR-02-007: OAuth2 성공 후 JWT 전달 방식

### 상태
결정됨

### 배경 및 문제

OAuth2 로그인은 서버-서버 Redirect 흐름이므로, 일반 로그인처럼 Response Body에
JWT를 담아 반환할 수 없다. SPA 프론트엔드에 JWT를 안전하게 전달하는 방법이 필요하다.

**고려 옵션:**

| 방식 | 흐름 | 보안 |
|------|------|------|
| **Query Parameter** | Redirect URL에 토큰 포함, 프론트에서 즉시 추출 후 URL 정리 | URL 노출 (브라우저 히스토리), 단순 구현 |
| HttpOnly Cookie | 쿠키에 토큰 설정 후 Redirect, 추가 API로 토큰 수신 | XSS 안전, CORS 쿠키 설정 복잡 |
| 임시 인증 코드 | 단기 코드 발급 → 프론트에서 코드로 토큰 교환 | 가장 안전, 구현 복잡 |

### 결정

**Query Parameter 방식을 사용하되, 프론트엔드에서 즉시 URL 정리를 필수로 한다.**

**OAuth2 성공 흐름 (`OAuth2SuccessHandler`):**
```
1. 사용자가 프론트에서 "Google로 로그인" 클릭
   → GET /oauth2/authorization/google

2. 구글 로그인 완료 → 백엔드 콜백
   → CustomOAuth2UserService에서 사용자 처리

3. JWT 발급 후 프론트엔드로 Redirect:
   http://localhost:3000/oauth2/callback
     ?accessToken={accessToken}
     &refreshToken={refreshToken}

4. 프론트엔드 OAuthCallback.tsx:
   a. URL에서 accessToken, refreshToken 파싱
   b. window.history.replaceState({}, '', '/') 로 URL 즉시 정리
   c. Zustand에 accessToken 저장
   d. localStorage에 refreshToken 저장
   e. 메인 페이지로 이동
```

**`OAuth2SuccessHandler.java` 핵심 구현:**
```java
String redirectUrl = UriComponentsBuilder
    .fromUriString(frontendUrl + "/oauth2/callback")
    .queryParam("accessToken", accessToken)
    .queryParam("refreshToken", refreshToken)
    .build().toUriString();

response.sendRedirect(redirectUrl);
```

### 근거

1. **구현 단순성**: 포트폴리오 수준에서 가장 빠르게 구현 가능하다.
2. **URL 즉시 정리**: `replaceState`로 브라우저 히스토리에서 토큰이 남지 않도록 한다.
3. **Access Token 만료**: 15분의 짧은 만료 시간으로 URL 노출 피해가 제한된다.
4. **추후 개선**: 서비스 규모가 커지면 임시 코드 방식으로 교체 가능하다.

### 영향

- `OAuth2SuccessHandler.java`: JWT 발급 후 프론트 URL로 Redirect
- `application-local.yaml`: `app.frontend-url: http://localhost:3000` 추가 필요
- 프론트엔드 `OAuthCallback.tsx`: URL 파싱 → `replaceState` → Zustand 저장 구현
- CORS 설정: `/oauth2/**` 경로 허용 필요

---

## 결정 요약

| ADR | 결정 내용 |
|-----|-----------|
| ADR-02-001 | JWT 전송: **Authorization: Bearer {accessToken}** |
| ADR-02-002 | Refresh Token: **Response Body 반환 + Redis `RT:{userId}`** |
| ADR-02-003 | Rotation: **RTR 완전 교체** (불일치 시 세션 전체 강제 종료) |
| ADR-02-004 | 해시: **BCrypt strength 10** |
| ADR-02-005 | Payload: **sub=userId + email + role** |
| ADR-02-006 | 이메일 중복: **에러 반환** / Kakao 미제공: **임시 이메일 자동 생성** |
| ADR-02-007 | OAuth2 전달: **Query Parameter → 프론트에서 즉시 URL 정리** |

---

*본 문서는 구현 진행에 따라 업데이트됩니다 | Last Updated: 2026-02-19*
