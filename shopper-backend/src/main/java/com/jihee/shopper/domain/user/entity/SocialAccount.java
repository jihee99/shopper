package com.jihee.shopper.domain.user.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 연동 정보 엔티티.
 * 한 User는 여러 소셜 계정(Google, Kakao)을 연동할 수 있다 (1:N).
 * (provider, providerId) 쌍은 유일해야 한다.
 */
@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    public static SocialAccount of(User user, SocialProvider provider, String providerId) {
        SocialAccount account = new SocialAccount();
        account.user = user;
        account.provider = provider;
        account.providerId = providerId;
        return account;
    }
}
