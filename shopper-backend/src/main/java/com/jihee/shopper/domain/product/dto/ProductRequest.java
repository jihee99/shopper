package com.jihee.shopper.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 등록/수정 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class ProductRequest {

    @NotNull(message = "카테고리는 필수입니다")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다")
    private String name;

    private String description;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;

    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    private Integer stock;
}
