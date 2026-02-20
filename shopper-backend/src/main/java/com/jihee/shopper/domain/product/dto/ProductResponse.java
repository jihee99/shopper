package com.jihee.shopper.domain.product.dto;

import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 상세 응답 DTO (이미지 포함).
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductResponse {

    private final Long id;
    private final Long categoryId;
    private final String categoryName;
    private final String name;
    private final String description;
    private final Integer price;
    private final Integer stock;
    private final ProductStatus status;
    private final Integer salesCount;
    private final List<ImageInfo> images;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        List<ImageInfo> images = product.getImages().stream()
                .map(img -> new ImageInfo(
                        img.getId(),
                        img.getUrl(),
                        img.isMain(),
                        img.getSortOrder()
                ))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getSalesCount(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class ImageInfo {
        private final Long id;
        private final String url;
        private final boolean isMain;
        private final Integer sortOrder;
    }
}
