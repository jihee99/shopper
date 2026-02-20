package com.jihee.shopper.domain.product.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품 엔티티.
 *
 * <p>ADR-03-002: 가격은 Integer (원 단위)
 * <p>ADR-03-003: 낙관적 락 (@Version) 사용
 * <p>ADR-03-004: 소프트 삭제 (status: ACTIVE/INACTIVE)
 * <p>ADR-03-009: 판매 수량 추적 (salesCount)
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 가격 (원 단위, ADR-03-002) */
    @Column(nullable = false)
    private Integer price;

    /** 재고 수량 */
    @Column(nullable = false)
    private Integer stock;

    /** 상품 상태 (ADR-03-004) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    /** 판매 수량 (ADR-03-009) */
    @Column(nullable = false)
    private Integer salesCount = 0;

    /** 낙관적 락 (ADR-03-003) */
    @Version
    private Long version;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    public static Product create(Category category, String name, String description,
                                  Integer price, Integer stock) {
        Product product = new Product();
        product.category = category;
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        product.status = ProductStatus.ACTIVE;
        product.salesCount = 0;
        return product;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    public void update(Category category, String name, String description,
                       Integer price, Integer stock) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    // ── 재고 관리 ───────────────────────────────────────────────────────────

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    // ── 판매 수량 관리 (ADR-03-009) ──────────────────────────────────────────

    public void increaseSalesCount(int quantity) {
        this.salesCount += quantity;
    }

    public void decreaseSalesCount(int quantity) {
        this.salesCount = Math.max(0, this.salesCount - quantity);
    }
}
