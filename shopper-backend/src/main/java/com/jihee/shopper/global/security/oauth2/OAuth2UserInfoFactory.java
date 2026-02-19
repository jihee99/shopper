package com.jihee.shopper.global.security.oauth2;

import java.util.Map;

/** registrationId를 기반으로 적절한 OAuth2UserInfo 구현체를 생성한다. */
public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 Provider: " + registrationId);
        };
    }
}
