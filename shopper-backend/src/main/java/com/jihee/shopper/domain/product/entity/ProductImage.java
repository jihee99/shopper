package com.jihee.shopper.domain.product.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 이미지 엔티티 (ADR-03-005).
 *
 * <p>S3 URL을 저장하며, 대표 이미지(isMain)와 정렬 순서(sortOrder)를 관리한다.
 */
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** S3 업로드 URL */
    @Column(nullable = false)
    private String url;

    /** 대표 이미지 여부 (ADR-03-005) */
    @Column(nullable = false)
    private boolean isMain;

    /** 정렬 순서 (0, 1, 2, ...) */
    @Column(nullable = false)
    private Integer sortOrder;

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    public static ProductImage of(Product product, String url, boolean isMain, Integer sortOrder) {
        ProductImage image = new ProductImage();
        image.product = product;
        image.url = url;
        image.isMain = isMain;
        image.sortOrder = sortOrder;
        return image;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    public void clearMain() {
        this.isMain = false;
    }

    public void markAsMain() {
        this.isMain = true;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
