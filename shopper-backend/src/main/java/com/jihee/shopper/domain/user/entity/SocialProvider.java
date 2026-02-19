package com.jihee.shopper.domain.user.entity;

/**
 * OAuth2 소셜 로그인 제공자.
 * registrationId("google", "kakao")를 대문자로 변환하여 매핑한다.
 */
public enum SocialProvider {
    GOOGLE,
    KAKAO
}
