package com.jihee.shopper.domain.cart;

import com.jihee.shopper.domain.cart.dto.CartItemRequest;
import com.jihee.shopper.domain.cart.dto.CartResponse;
import com.jihee.shopper.domain.cart.entity.Cart;
import com.jihee.shopper.domain.cart.entity.CartItem;
import com.jihee.shopper.domain.product.CategoryRepository;
import com.jihee.shopper.domain.product.ProductRepository;
import com.jihee.shopper.domain.product.entity.Category;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.createSocialUser("test@example.com", "Test User");
        userRepository.save(testUser);

        testCategory = Category.createRoot("전자제품");
        categoryRepository.save(testCategory);

        testProduct = Product.create(testCategory, "맥북 프로", "고성능 노트북", 2500000, 10);
        productRepository.save(testProduct);
    }

    // ── 장바구니 조회 ───────────────────────────────────────────────────

    @Test
    @DisplayName("장바구니 조회 성공 - 최초 접근 시 자동 생성")
    void getCart_Success_AutoCreate() {
        // when
        CartResponse response = cartService.getCart(testUser.getId());

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalPrice()).isZero();

        // 장바구니가 자동 생성되었는지 확인
        assertThat(cartRepository.findByUserId(testUser.getId())).isPresent();
    }

    @Test
    @DisplayName("장바구니 조회 성공 - 기존 장바구니")
    void getCart_Success_ExistingCart() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct, 2);
        cartItemRepository.save(cartItem);

        // when
        CartResponse response = cartService.getCart(testUser.getId());

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("맥북 프로");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualTo(5000000);
    }

    @Test
    @DisplayName("장바구니 조회 - INACTIVE 상품 필터링")
    void getCart_FilterInactiveProducts() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        Product inactiveProduct = Product.create(testCategory, "LG 그램", "초경량 노트북", 1800000, 5);
        inactiveProduct.deactivate();
        productRepository.save(inactiveProduct);

        CartItem cartItem1 = CartItem.of(cart, testProduct, 1);
        CartItem cartItem2 = CartItem.of(cart, inactiveProduct, 1);
        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);

        // when
        CartResponse response = cartService.getCart(testUser.getId());

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("맥북 프로");
    }

    // ── 장바구니 상품 추가 ──────────────────────────────────────────────

    @Test
    @DisplayName("장바구니 상품 추가 성공 - 새 상품")
    void addToCart_Success_NewProduct() {
        // given
        CartItemRequest request = new CartItemRequest(testProduct.getId(), 2);

        // when
        cartService.addToCart(testUser.getId(), request);

        // then
        CartResponse response = cartService.getCart(testUser.getId());
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("장바구니 상품 추가 성공 - 중복 상품 수량 증가")
    void addToCart_Success_DuplicateProduct() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem existingItem = CartItem.of(cart, testProduct, 2);
        cartItemRepository.save(existingItem);

        CartItemRequest request = new CartItemRequest(testProduct.getId(), 3);

        // when
        cartService.addToCart(testUser.getId(), request);

        // then
        CartResponse response = cartService.getCart(testUser.getId());
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);  // 2 + 3
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - 재고 부족")
    void addToCart_Fail_OutOfStock() {
        // given
        CartItemRequest request = new CartItemRequest(testProduct.getId(), 15);  // 재고 10개

        // when & then
        assertThatThrownBy(() -> cartService.addToCart(testUser.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("장바구니 상품 추가 실패 - INACTIVE 상품")
    void addToCart_Fail_InactiveProduct() {
        // given
        testProduct.deactivate();

        CartItemRequest request = new CartItemRequest(testProduct.getId(), 2);

        // when & then
        assertThatThrownBy(() -> cartService.addToCart(testUser.getId(), request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    // ── 장바구니 상품 수량 변경 ─────────────────────────────────────────

    @Test
    @DisplayName("장바구니 상품 수량 변경 성공")
    void updateQuantity_Success() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct, 2);
        cartItemRepository.save(cartItem);

        // when
        cartService.updateQuantity(testUser.getId(), cartItem.getId(), 5);

        // then
        CartItem updated = cartItemRepository.findById(cartItem.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("장바구니 상품 수량 변경 실패 - 재고 부족")
    void updateQuantity_Fail_OutOfStock() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct, 2);
        cartItemRepository.save(cartItem);

        // when & then
        assertThatThrownBy(() -> cartService.updateQuantity(testUser.getId(), cartItem.getId(), 15))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    // ── 장바구니 상품 삭제 ──────────────────────────────────────────────

    @Test
    @DisplayName("장바구니 상품 삭제 성공")
    void removeFromCart_Success() {
        // given
        Cart cart = Cart.createForUser(testUser);
        cartRepository.save(cart);

        CartItem cartItem = CartItem.of(cart, testProduct, 2);
        cartItemRepository.save(cartItem);

        // when
        cartService.removeFromCart(testUser.getId(), cartItem.getId());

        // then
        assertThat(cartItemRepository.findById(cartItem.getId())).isEmpty();
    }
}
