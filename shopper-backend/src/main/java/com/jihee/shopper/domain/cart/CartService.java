package com.jihee.shopper.domain.cart;

import com.jihee.shopper.domain.cart.dto.CartItemRequest;
import com.jihee.shopper.domain.cart.dto.CartResponse;
import com.jihee.shopper.domain.cart.entity.Cart;
import com.jihee.shopper.domain.cart.entity.CartItem;
import com.jihee.shopper.domain.product.ProductRepository;
import com.jihee.shopper.domain.product.entity.Product;
import com.jihee.shopper.domain.product.entity.ProductStatus;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 장바구니 서비스 (ADR-04-001 ~ ADR-04-005).
 *
 * <p>장바구니 자동 생성, 중복 상품 처리, 재고 검증을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // ── 장바구니 조회 ──────────────────────────────────────────────────────

    /**
     * 장바구니 조회 (ADR-04-001: 최초 접근 시 자동 생성).
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return CartResponse.of(items);  // ACTIVE 상품만 필터링 (DTO 내부)
    }

    // ── 장바구니 상품 추가 ──────────────────────────────────────────────────

    /**
     * 장바구니 상품 추가 (ADR-04-002: 중복 시 수량 증가).
     */
    @Transactional
    public void addToCart(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = findProductById(request.getProductId());

        // 상품 상태 검증
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 재고 검증 (ADR-04-003)
        if (request.getQuantity() > product.getStock()) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        // 기존 CartItem 확인
        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existing.isPresent()) {
            // 중복 상품: 수량 증가 (ADR-04-002)
            CartItem cartItem = existing.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            // 재고 재검증
            if (newQuantity > product.getStock()) {
                throw new CustomException(ErrorCode.OUT_OF_STOCK);
            }

            cartItem.increaseQuantity(request.getQuantity());
        } else {
            // 새 CartItem 생성
            CartItem cartItem = CartItem.of(cart, product, request.getQuantity());
            cartItemRepository.save(cartItem);
        }
    }

    // ── 장바구니 상품 수량 변경 ─────────────────────────────────────────────

    /**
     * 장바구니 상품 수량 변경.
     */
    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem cartItem = findCartItemByIdAndUserId(cartItemId, userId);

        // 수량 검증
        if (quantity < 1) {
            throw new CustomException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        }

        // 재고 검증 (ADR-04-003)
        if (quantity > cartItem.getProduct().getStock()) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        cartItem.updateQuantity(quantity);
    }

    // ── 장바구니 상품 삭제 ──────────────────────────────────────────────────

    /**
     * 장바구니 상품 삭제.
     */
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem cartItem = findCartItemByIdAndUserId(cartItemId, userId);
        cartItemRepository.delete(cartItem);
    }

    // ── 내부 공용 ──────────────────────────────────────────────────────────

    /**
     * 장바구니 조회 또는 생성 (ADR-04-001).
     */
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    Cart cart = Cart.createForUser(user);
                    return cartRepository.save(cart);
                });
    }

    /**
     * CartItem 조회 및 권한 검증.
     */
    private CartItem findCartItemByIdAndUserId(Long cartItemId, Long userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 본인 장바구니 확인
        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cartItem;
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
