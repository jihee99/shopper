package com.jihee.shopper.domain.user.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 회원 엔티티.
 * - 일반 가입: email + password (BCrypt)
 * - 소셜 가입: email + password(null) + SocialAccount 연결
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** 소셜 전용 사용자는 null */
    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    /** 이메일 + 비밀번호 일반 회원가입 */
    public static User createNormalUser(String email, String encodedPassword, String name) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        user.role = UserRole.ROLE_USER;
        return user;
    }

    /** 소셜 로그인 신규 가입 */
    public static User createSocialUser(String email, String name) {
        User user = new User();
        user.email = email;
        user.password = null;
        user.name = name;
        user.role = UserRole.ROLE_USER;
        return user;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
