package com.jihee.shopper.domain.user;

import com.jihee.shopper.domain.user.entity.SocialAccount;
import com.jihee.shopper.domain.user.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
