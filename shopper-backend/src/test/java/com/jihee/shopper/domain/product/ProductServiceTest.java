package com.jihee.shopper.domain.product;

import com.jihee.shopper.domain.product.dto.ProductRequest;
import com.jihee.shopper.domain.product.dto.ProductResponse;
import com.jihee.shopper.domain.product.entity.Category;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category rootCategory;
    private Category subCategory;

    @BeforeEach
    void setUp() {
        rootCategory = Category.createRoot("전자제품");
        categoryRepository.save(rootCategory);

        subCategory = Category.createChild(rootCategory, "노트북");
        categoryRepository.save(subCategory);
    }

    // ── 상품 조회 (공개 API) ────────────────────────────────────────────

    @Test
    @DisplayName("상품 목록 조회 성공 - 필터 없음")
    void getProducts_Success_NoFilter() {
        // given
        Product product1 = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        Product product2 = Product.create(subCategory, "LG 그램", "초경량 노트북", 1800000, 5);
        productRepository.save(product1);
        productRepository.save(product2);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<?> responses = productService.getProducts(null, pageable);

        // then
        assertThat(responses.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("상품 목록 조회 - INACTIVE 상품 제외")
    void getProducts_ExcludeInactiveProducts() {
        // given
        Product product1 = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        Product product2 = Product.create(subCategory, "LG 그램", "초경량 노트북", 1800000, 5);
        product2.deactivate();
        productRepository.save(product1);
        productRepository.save(product2);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<?> responses = productService.getProducts(null, pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProduct_Success() {
        // given
        Product product = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        productRepository.save(product);

        // when
        ProductResponse response = productService.getProduct(product.getId());

        // then
        assertThat(response.getName()).isEqualTo("맥북 프로");
        assertThat(response.getPrice()).isEqualTo(2500000);
        assertThat(response.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - INACTIVE 상품")
    void getProduct_Fail_InactiveProduct() {
        // given
        Product product = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        product.deactivate();
        productRepository.save(product);

        // when & then
        assertThatThrownBy(() -> productService.getProduct(product.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    // ── 상품 관리 (관리자 API) ──────────────────────────────────────────

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_Success() {
        // given
        ProductRequest request = new ProductRequest();
        request.setCategoryId(subCategory.getId());
        request.setName("맥북 프로");
        request.setDescription("고성능 노트북");
        request.setPrice(2500000);
        request.setStock(10);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response.getName()).isEqualTo("맥북 프로");
        assertThat(response.getPrice()).isEqualTo(2500000);
        assertThat(response.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_Success() {
        // given
        Product product = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        productRepository.save(product);

        ProductRequest request = new ProductRequest();
        request.setCategoryId(subCategory.getId());
        request.setName("맥북 프로 M3");
        request.setDescription("최신 M3 칩 탑재");
        request.setPrice(2800000);
        request.setStock(15);

        // when
        ProductResponse response = productService.updateProduct(product.getId(), request);

        // then
        assertThat(response.getName()).isEqualTo("맥북 프로 M3");
        assertThat(response.getPrice()).isEqualTo(2800000);
        assertThat(response.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("상품 삭제 성공 - Soft Delete")
    void deleteProduct_Success() {
        // given
        Product product = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        productRepository.save(product);

        // when
        productService.deleteProduct(product.getId());

        // then
        Product deleted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("상품 재고 증가/감소")
    void updateStock() {
        // given
        Product product = Product.create(subCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        productRepository.save(product);

        // when - 재고 감소
        product.decreaseStock(3);

        // then
        assertThat(product.getStock()).isEqualTo(7);

        // when - 재고 증가
        product.increaseStock(5);

        // then
        assertThat(product.getStock()).isEqualTo(12);
    }
}
