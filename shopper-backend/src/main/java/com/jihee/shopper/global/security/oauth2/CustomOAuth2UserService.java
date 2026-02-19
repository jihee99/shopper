package com.jihee.shopper.global.security.oauth2;

import com.jihee.shopper.domain.user.SocialAccountRepository;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.SocialAccount;
import com.jihee.shopper.domain.user.entity.SocialProvider;
import com.jihee.shopper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 로그인 처리 서비스 (ADR-02-006).
 *
 * <p>처리 흐름:
 * <ol>
 *   <li>소셜 Provider에서 사용자 정보 수신</li>
 *   <li>기존 SocialAccount 존재 → 로그인 처리</li>
 *   <li>신규: email 중복 확인 → 중복이면 에러 / 아니면 User + SocialAccount 생성</li>
 *   <li>Kakao email 미제공 → 임시 이메일 자동 생성</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.getAttributes());
        User user = processOAuth2User(userInfo, registrationId);

        return new CustomOAuth2User(oAuth2User, user, userNameAttributeName);
    }

    @Transactional
    protected User processOAuth2User(OAuth2UserInfo userInfo, String registrationId) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());

        // 기존 소셜 계정 확인 → 있으면 해당 User 반환
        return socialAccountRepository
                .findByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(SocialAccount::getUser)
                .orElseGet(() -> registerNewSocialUser(userInfo, provider));
    }

    private User registerNewSocialUser(OAuth2UserInfo userInfo, SocialProvider provider) {
        // Kakao email 미제공 시 임시 이메일 생성 (ADR-02-006)
        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            email = provider.name().toLowerCase() + "_" + userInfo.getProviderId() + "@social.shopper";
        }

        final String finalEmail = email;

        // 동일 email의 일반 계정 존재 → 에러 (ADR-02-006: 자동 연동 금지)
        userRepository.findByEmail(finalEmail).ifPresent(existing -> {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_already_exists"),
                    "이미 해당 이메일로 가입된 일반 계정이 있습니다. 일반 로그인을 이용해주세요."
            );
        });

        User user = User.createSocialUser(finalEmail, userInfo.getName());
        User savedUser = userRepository.save(user);

        SocialAccount socialAccount = SocialAccount.of(savedUser, provider, userInfo.getProviderId());
        socialAccountRepository.save(socialAccount);

        return savedUser;
    }
}
