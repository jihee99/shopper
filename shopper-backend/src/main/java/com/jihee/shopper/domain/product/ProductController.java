package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.dto.ProductListResponse;
import com.jihee.shopper.domain.product.dto.ProductResponse;
import com.jihee.shopper.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 공개 API 컨트롤러.
 *
 * <pre>
 * GET /api/products       — 상품 목록 (페이징, 카테고리 필터)
 * GET /api/products/{id}  — 상품 상세
 * </pre>
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록 조회 (공개).
     *
     * @param categoryId 카테고리 ID (선택적)
     * @param pageable   페이징 정보 (기본: page=0, size=20, sort=createdAt,DESC)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductListResponse> products = productService.getProducts(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 상품 상세 조회 (공개).
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        ProductResponse product = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
