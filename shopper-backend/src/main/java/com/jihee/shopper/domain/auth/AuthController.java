package com.jihee.shopper.domain.auth;

import com.jihee.shopper.domain.auth.dto.LoginRequest;
import com.jihee.shopper.domain.auth.dto.RefreshRequest;
import com.jihee.shopper.domain.auth.dto.SignupRequest;
import com.jihee.shopper.domain.auth.dto.TokenResponse;
import com.jihee.shopper.global.common.ApiResponse;
import com.jihee.shopper.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API 컨트롤러.
 *
 * <pre>
 * POST /api/auth/signup   — 회원가입
 * POST /api/auth/login    — 로그인 (Access + Refresh Token 발급)
 * POST /api/auth/refresh  — Access Token 재발급 (RTR)
 * POST /api/auth/logout   — 로그아웃 (Redis Refresh Token 삭제)
 * </pre>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다"));
    }

    /** 로그인 → Access Token + Refresh Token 반환 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    /** Refresh Token으로 Access Token 재발급 (RTR: 두 토큰 모두 교체) */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다", response));
    }

    /** 로그아웃 → Redis에서 Refresh Token 삭제 (인증 필요) */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다"));
    }
}
