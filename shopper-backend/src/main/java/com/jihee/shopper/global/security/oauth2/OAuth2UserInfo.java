package com.jihee.shopper.global.security.oauth2;

/**
 * 소셜 Provider별 사용자 정보 추상화 인터페이스.
 * Google / Kakao의 attribute 구조 차이를 감춘다.
 */
public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();      // Kakao는 null일 수 있음 (ADR-02-006)
    String getName();
}
