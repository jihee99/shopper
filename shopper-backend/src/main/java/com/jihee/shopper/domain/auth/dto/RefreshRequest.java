package com.jihee.shopper.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotBlank(message = "Refresh Token은 필수입니다")
    private String refreshToken;
}
