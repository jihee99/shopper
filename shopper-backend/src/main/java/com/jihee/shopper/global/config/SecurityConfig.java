package com.jihee.shopper.global.config;

import com.jihee.shopper.global.security.JwtFilter;
import com.jihee.shopper.global.security.JwtProvider;
import com.jihee.shopper.global.security.oauth2.CustomOAuth2UserService;
import com.jihee.shopper.global.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 필터 체인 설정.
 *
 * <p>인증 전략 (ADR-02-001):
 * - Stateless JWT 기반
 * - Authorization: Bearer {accessToken} 헤더로 인증
 * - OAuth2 소셜 로그인 (Google, Kakao) 지원
 *
 * <p>URL 권한 정책:
 * - /api/auth/**: 인증 불필요 (회원가입, 로그인, 토큰 재발급)
 * - GET /api/products/**: 인증 불필요 (상품 조회는 공개)
 * - /api/admin/**: ROLE_ADMIN 전용
 * - 그 외: 인증 필요
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // REST API → CSRF 불필요
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 (CorsConfigurationSource Bean 사용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless 세션 (JWT 기반)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // OAuth2 소셜 로그인
//            .oauth2Login(oauth2 -> oauth2
//                .userInfoEndpoint(userInfo ->
//                    userInfo.userService(customOAuth2UserService))
//                .successHandler(oauth2SuccessHandler)
//            )

            // 인증 실패 / 권한 없음 → JSON 응답
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"
                    );
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다\"}"
                    );
                })
            )

            // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt 비밀번호 인코더 (ADR-02-004: strength 10) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS 설정 (ADR-02-001: Authorization 헤더 허용) */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false); // Bearer 헤더 방식 → credentials 불필요

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
