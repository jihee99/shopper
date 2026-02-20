package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 상품 이미지 리포지토리.
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * 특정 상품의 모든 이미지 조회 (정렬 순서대로).
     */
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    /**
     * 특정 상품의 메인 이미지 조회.
     */
    Optional<ProductImage> findByProductIdAndIsMainTrue(Long productId);

    /**
     * 특정 상품의 기존 메인 이미지를 모두 해제 (ADR-03-005).
     */
    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isMain = false WHERE pi.product.id = :productId AND pi.isMain = true")
    void clearMainByProductId(Long productId);
}
