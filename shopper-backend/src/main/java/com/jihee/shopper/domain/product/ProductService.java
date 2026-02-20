package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.dto.ProductListResponse;
import com.jihee.shopper.domain.product.dto.ProductRequest;
import com.jihee.shopper.domain.product.dto.ProductResponse;
import com.jihee.shopper.domain.product.entity.Category;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductImage;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import com.jihee.shopper.infra.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 상품 서비스.
 *
 * <p>공개 조회 API와 관리자 CRUD를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_IMAGES_PER_PRODUCT = 10;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final S3Uploader s3Uploader;

    // ── 공개 조회 API ───────────────────────────────────────────────────────

    /**
     * 상품 목록 조회 (카테고리 필터 선택적, ACTIVE 상품만).
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Long categoryId, Pageable pageable) {
        Page<Product> products;

        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndStatus(
                    categoryId, ProductStatus.ACTIVE, pageable);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }

        return products.map(ProductListResponse::from);
    }

    /**
     * 상품 상세 조회 (ACTIVE 상품만).
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return ProductResponse.from(product);
    }

    // ── 관리자 CRUD ─────────────────────────────────────────────────────────

    /**
     * 상품 등록 (관리자).
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = findCategoryById(request.getCategoryId());

        Product product = Product.create(
                category,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock()
        );

        productRepository.save(product);
        return ProductResponse.from(product);
    }

    /**
     * 상품 수정 (관리자, ADR-03-017: 카테고리 변경 허용).
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = findProductById(productId);
        Category category = findCategoryById(request.getCategoryId());

        product.update(
                category,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock()
        );

        return ProductResponse.from(product);
    }

    /**
     * 상품 삭제 (관리자, ADR-03-004: 소프트 삭제).
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductById(productId);
        product.deactivate();  // status = INACTIVE (이미지는 유지, ADR-03-018)
    }

    // ── 이미지 업로드 ───────────────────────────────────────────────────────

    /**
     * 상품 이미지 업로드 (관리자, ADR-03-012 ~ ADR-03-014).
     *
     * @param productId 상품 ID
     * @param file      이미지 파일
     * @param isMain    메인 이미지 여부 (첫 이미지는 자동 true)
     * @return 업로드된 이미지 URL
     */
    @Transactional
    public String uploadProductImage(Long productId, MultipartFile file, boolean isMain) {
        Product product = findProductById(productId);

        // 이미지 개수 제한 검증 (ADR-03-014: 최대 10개)
        long imageCount = productImageRepository.findByProductIdOrderBySortOrderAsc(productId).size();
        if (imageCount >= MAX_IMAGES_PER_PRODUCT) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        // S3 업로드
        String imageUrl = s3Uploader.uploadImage(file);

        // 첫 이미지는 자동으로 메인 이미지 (ADR-03-013)
        boolean shouldBeMain = isMain;
        if (imageCount == 0) {
            shouldBeMain = true;
        }

        // 메인 이미지 지정 시 기존 메인 해제
        if (shouldBeMain) {
            productImageRepository.clearMainByProductId(productId);
        }

        // ProductImage 엔티티 저장
        ProductImage productImage = ProductImage.of(
                product,
                imageUrl,
                shouldBeMain,
                (int) imageCount  // sortOrder
        );
        productImageRepository.save(productImage);

        return imageUrl;
    }

    /**
     * 상품 이미지 삭제 (관리자, ADR-03-006: S3 + DB 즉시 삭제).
     */
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        // 상품 소유 확인
        if (!image.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND);
        }

        // S3 삭제 먼저 (실패 시 트랜잭션 롤백)
        s3Uploader.deleteImage(image.getUrl());

        // DB 삭제
        productImageRepository.delete(image);
    }

    // ── 내부 공용 ──────────────────────────────────────────────────────────

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
