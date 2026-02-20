package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.dto.ProductRequest;
import com.jihee.shopper.domain.product.dto.ProductResponse;
import com.jihee.shopper.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자 상품 관리 API 컨트롤러 (ADR-03-012 ~ ADR-03-020).
 *
 * <pre>
 * POST   /api/admin/products              — 상품 등록
 * PUT    /api/admin/products/{id}         — 상품 수정
 * DELETE /api/admin/products/{id}         — 상품 삭제 (소프트 삭제)
 * POST   /api/admin/products/{id}/images  — 이미지 업로드
 * DELETE /api/admin/products/{id}/images/{imageId} — 이미지 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 상품 등록 (관리자).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("상품이 등록되었습니다", response));
    }

    /**
     * 상품 수정 (관리자).
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("상품이 수정되었습니다", response));
    }

    /**
     * 상품 삭제 (관리자, 소프트 삭제).
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다"));
    }

    /**
     * 상품 이미지 업로드 (관리자, ADR-03-012).
     *
     * @param productId 상품 ID
     * @param file      이미지 파일 (multipart/form-data)
     * @param isMain    메인 이미지 여부 (기본값: false, 첫 이미지는 자동 true)
     */
    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean isMain) {
        String imageUrl = productService.uploadProductImage(productId, file, isMain);
        ImageUploadResponse response = new ImageUploadResponse(imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이미지가 업로드되었습니다", response));
    }

    /**
     * 상품 이미지 삭제 (관리자, ADR-03-006).
     */
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다"));
    }

    // ── 응답 DTO ───────────────────────────────────────────────────────────

    /**
     * 이미지 업로드 응답.
     */
    public record ImageUploadResponse(String imageUrl) {
    }
}
