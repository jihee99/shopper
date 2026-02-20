package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 상품 리포지토리.
 *
 * <p>QueryDSL 동적 쿼리는 ProductRepositoryCustom으로 확장 예정.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 카테고리별 상품 목록 조회 (상태 필터링).
     */
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    /**
     * 상태별 상품 목록 조회.
     */
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * 특정 카테고리의 상품 수 조회 (ADR-03-008: 카테고리 삭제 검증용).
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(Long categoryId);
}
