package com.jihee.shopper.domain.order;

import com.jihee.shopper.domain.order.dto.OrderRequest;
import com.jihee.shopper.domain.order.dto.OrderResponse;
import com.jihee.shopper.global.common.ApiResponse;
import com.jihee.shopper.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 API 컨트롤러.
 *
 * <pre>
 * POST /api/orders      — 주문 생성 (재고 차감, 장바구니 비우기)
 * GET  /api/orders/me   — 내 주문 내역 (페이징)
 * GET  /api/orders/{id} — 주문 상세
 * DELETE /api/orders/{id} — 주문 취소 (PENDING 상태만)
 * </pre>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (재고 차감, OrderItem 스냅샷, 장바구니 비우기).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 생성되었습니다", response));
    }

    /**
     * 내 주문 내역 조회 (페이징).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderResponse> response = orderService.getMyOrders(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 주문 상세 조회.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getOrder(userDetails.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 주문 취소 (PENDING 상태만 재고 복구).
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        orderService.cancelOrder(userDetails.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다"));
    }
}
