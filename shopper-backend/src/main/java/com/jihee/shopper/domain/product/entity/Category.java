package com.jihee.shopper.domain.product.entity;

import com.jihee.shopper.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 카테고리 엔티티 (ADR-03-001).
 *
 * <p>계층형 구조 (self-join) 지원, 최대 3단계 깊이 제한.
 * <pre>
 * depth 0: 대분류 (예: 전자제품)
 * depth 1: 중분류 (예: 스마트폰)
 * depth 2: 소분류 (예: 갤럭시 시리즈)
 * </pre>
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 부모 카테고리 (nullable: 최상위 카테고리는 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private String name;

    /** 계층 깊이 (0~2) */
    @Column(nullable = false)
    private Integer depth;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    // ── 정적 팩토리 메서드 ───────────────────────────────────────────────────

    /** 최상위 카테고리 생성 (depth 0) */
    public static Category createRoot(String name) {
        Category category = new Category();
        category.name = name;
        category.depth = 0;
        category.parent = null;
        return category;
    }

    /** 하위 카테고리 생성 (parent의 depth + 1) */
    public static Category createChild(Category parent, String name) {
        Category category = new Category();
        category.parent = parent;
        category.name = name;
        category.depth = parent.depth + 1;
        return category;
    }

    // ── 수정 메서드 ─────────────────────────────────────────────────────────

    public void updateName(String name) {
        this.name = name;
    }
}
