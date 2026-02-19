package com.jihee.shopper.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AddressRequest {

    @NotBlank(message = "배송지명은 필수입니다")
    private String name;

    @NotBlank(message = "수령인은 필수입니다")
    private String recipient;

    @NotBlank(message = "연락처는 필수입니다")
    private String phone;

    @NotBlank(message = "우편번호는 필수입니다")
    private String zipCode;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    private String addressDetail;

    private boolean isDefault;
}
