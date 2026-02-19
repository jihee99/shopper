package com.jihee.shopper.domain.user;

import com.jihee.shopper.domain.user.dto.AddressRequest;
import com.jihee.shopper.domain.user.dto.AddressResponse;
import com.jihee.shopper.domain.user.dto.UserResponse;
import com.jihee.shopper.domain.user.dto.UserUpdateRequest;
import com.jihee.shopper.global.common.ApiResponse;
import com.jihee.shopper.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회원 API 컨트롤러.
 *
 * <pre>
 * GET    /api/users/me                   — 내 정보 조회
 * PUT    /api/users/me                   — 내 정보 수정
 * GET    /api/users/me/addresses         — 배송지 목록 조회
 * POST   /api/users/me/addresses         — 배송지 추가
 * DELETE /api/users/me/addresses/{id}    — 배송지 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = userService.getMe(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 내 정보 수정 */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateMe(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("회원 정보가 수정되었습니다", response));
    }

    /** 배송지 목록 조회 */
    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AddressResponse> response = userService.getAddresses(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 배송지 추가 */
    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userService.addAddress(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("배송지가 추가되었습니다", response));
    }

    /** 배송지 삭제 */
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        userService.deleteAddress(userDetails.getUserId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("배송지가 삭제되었습니다"));
    }
}
