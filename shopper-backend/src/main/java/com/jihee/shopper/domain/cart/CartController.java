package com.jihee.shopper.domain.cart;

import com.jihee.shopper.domain.cart.dto.CartItemRequest;
import com.jihee.shopper.domain.cart.dto.CartResponse;
import com.jihee.shopper.global.common.ApiResponse;
import com.jihee.shopper.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 장바구니 API 컨트롤러.
 *
 * <pre>
 * GET    /api/cart              — 장바구니 조회
 * POST   /api/cart/items        — 상품 추가
 * PUT    /api/cart/items/{id}   — 수량 변경
 * DELETE /api/cart/items/{id}   — 상품 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 조회.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse response = cartService.getCart(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 장바구니 상품 추가 (중복 시 수량 증가).
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        cartService.addToCart(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("장바구니에 추가되었습니다"));
    }

    /**
     * 장바구니 상품 수량 변경.
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        cartService.updateQuantity(userDetails.getUserId(), cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("수량이 변경되었습니다"));
    }

    /**
     * 장바구니 상품 삭제.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId) {
        cartService.removeFromCart(userDetails.getUserId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다"));
    }
}
