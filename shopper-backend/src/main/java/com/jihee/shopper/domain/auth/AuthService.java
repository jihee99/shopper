package com.jihee.shopper.domain.auth;

import com.jihee.shopper.domain.auth.dto.LoginRequest;
import com.jihee.shopper.domain.auth.dto.SignupRequest;
import com.jihee.shopper.domain.auth.dto.TokenResponse;
import com.jihee.shopper.domain.user.UserRepository;
import com.jihee.shopper.domain.user.entity.User;
import com.jihee.shopper.global.exception.CustomException;
import com.jihee.shopper.global.exception.ErrorCode;
import com.jihee.shopper.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 인증 서비스.
 *
 * <p>회원가입, 로그인, 토큰 재발급(RTR), 로그아웃을 처리한다.
 * Refresh Token은 Redis에 "RT:{userId}" 키로 저장된다 (ADR-02-002).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RT_PREFIX = "RT:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    // ── 회원가입 ────────────────────────────────────────────────────────────

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createNormalUser(request.getEmail(), encodedPassword, request.getName());
        userRepository.save(user);
    }

    // ── 로그인 ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 소셜 전용 계정(password == null) 또는 비밀번호 불일치
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return generateAndStoreTokens(user);
    }

    // ── 토큰 재발급 (RTR) ────────────────────────────────────────────────────

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        // 1. 서명 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String redisKey = RT_PREFIX + userId;

        // 2. Redis에서 저장된 토큰 조회
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 3. RTR: 요청 토큰 vs Redis 저장 토큰 일치 확인 (ADR-02-003)
        if (!storedToken.equals(refreshToken)) {
            // 탈취 의심 → 해당 유저 세션 전체 강제 종료
            redisTemplate.delete(redisKey);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 4. 유저 조회 후 새 토큰 발급
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return generateAndStoreTokens(user);
    }

    // ── 로그아웃 ────────────────────────────────────────────────────────────

    public void logout(Long userId) {
        redisTemplate.delete(RT_PREFIX + userId);
    }

    // ── 내부 공용 메서드 ────────────────────────────────────────────────────

    private TokenResponse generateAndStoreTokens(User user) {
        String accessToken  = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // Redis에 새 Refresh Token 저장 (기존 토큰 자동 덮어쓰기)
        redisTemplate.opsForValue().set(
                RT_PREFIX + user.getId(),
                refreshToken,
                Duration.ofMillis(jwtProvider.getRefreshTokenExpiry())
        );

        return TokenResponse.of(accessToken, refreshToken);
    }
}
