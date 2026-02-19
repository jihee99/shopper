package com.jihee.shopper.domain.user.dto;

import com.jihee.shopper.domain.user.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String name;
    private String recipient;
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private boolean isDefault;

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getName(),
                address.getRecipient(),
                address.getPhone(),
                address.getZipCode(),
                address.getAddress(),
                address.getAddressDetail(),
                address.isDefault()
        );
    }
}
