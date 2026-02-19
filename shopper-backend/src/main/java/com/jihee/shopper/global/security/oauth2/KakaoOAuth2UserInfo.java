package com.jihee.shopper.global.security.oauth2;

import java.util.Map;

/**
 * Kakao OAuth2 사용자 정보.
 * Kakao userinfo endpoint attributes (user-name-attribute: id):
 * {
 *   "id": 12345678,
 *   "kakao_account": {
 *     "email": "user@kakao.com",   // 선택 제공 → null 가능 (ADR-02-006)
 *     "profile": { "nickname": "닉네임" }
 *   }
 * }
 */
@SuppressWarnings("unchecked")
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return "카카오사용자";
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) return "카카오사용자";
        String nickname = (String) profile.get("nickname");
        return nickname != null ? nickname : "카카오사용자";
    }
}
