package com.jihee.shopper.domain.product.dto;

import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상품 목록 응답 DTO (간략 정보).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductListResponse {

    private final Long id;
    private final String name;
    private final Integer price;
    private final Integer stock;
    private final ProductStatus status;
    private final Integer salesCount;
    private final String mainImageUrl;  // 대표 이미지 URL

    public static ProductListResponse from(Product product) {
        String mainImageUrl = product.getImages().stream()
                .filter(img -> img.isMain())
                .findFirst()
                .map(img -> img.getUrl())
                .orElse(null);

        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getSalesCount(),
                mainImageUrl
        );
    }
}
