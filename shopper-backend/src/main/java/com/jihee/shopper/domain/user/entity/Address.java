package com.jihee.shopper.domain.user.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송지 엔티티.
 * 사용자는 여러 배송지를 등록할 수 있으며, 하나를 기본 배송지로 지정할 수 있다.
 */
@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;          // 배송지명 (집, 회사 등)

    @Column(nullable = false)
    private String recipient;     // 수령인

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String zipCode;       // 우편번호

    @Column(nullable = false)
    private String address;       // 기본 주소

    @Column
    private String addressDetail; // 상세 주소

    @Column(nullable = false)
    private boolean isDefault;    // 기본 배송지 여부

    public static Address of(User user, String name, String recipient, String phone,
                             String zipCode, String address, String addressDetail, boolean isDefault) {
        Address addr = new Address();
        addr.user = user;
        addr.name = name;
        addr.recipient = recipient;
        addr.phone = phone;
        addr.zipCode = zipCode;
        addr.address = address;
        addr.addressDetail = addressDetail;
        addr.isDefault = isDefault;
        return addr;
    }

    public void update(String name, String recipient, String phone,
                       String zipCode, String address, String addressDetail) {
        this.name = name;
        this.recipient = recipient;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    public void clearDefault() {
        this.isDefault = false;
    }

    public void markDefault() {
        this.isDefault = true;
    }
}
