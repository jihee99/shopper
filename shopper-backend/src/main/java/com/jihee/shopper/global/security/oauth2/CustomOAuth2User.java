package com.jihee.shopper.global.security.oauth2;

import com.jihee.shopper.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 인증 성공 후 SecurityContext에 저장되는 인증 객체.
 * OAuth2SuccessHandler에서 User 엔티티를 꺼내 JWT를 발급한다.
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final User user;
    private final String userNameAttributeName;

    public CustomOAuth2User(OAuth2User oauth2User, User user, String userNameAttributeName) {
        this.oauth2User = oauth2User;
        this.user = user;
        this.userNameAttributeName = userNameAttributeName;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    @Override
    public String getName() {
        return String.valueOf(getAttributes().get(userNameAttributeName));
    }

    /** JWT 발급에 사용할 도메인 User 엔티티 반환 */
    public User getUser() {
        return user;
    }
}
