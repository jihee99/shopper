package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 카테고리 리포지토리.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 최상위 카테고리 목록 조회 (depth = 0).
     */
    List<Category> findByDepth(Integer depth);

    /**
     * 부모 카테고리로 하위 카테고리 조회.
     */
    List<Category> findByParentId(Long parentId);

    /**
     * 특정 부모의 자식 카테고리 수 조회.
     */
    @Query("SELECT COUNT(c) FROM Category c WHERE c.parent.id = :parentId")
    long countByParentId(Long parentId);
}
