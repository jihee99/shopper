package com.jihee.shopper.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정 클래스.
 *
 * <p>{@code @EnableJpaAuditing}을 활성화하여 {@link com.jihee.shopper.global.common.BaseEntity}의
 * {@code createdAt}, {@code updatedAt} 필드가 자동으로 채워지도록 한다.
 *
 * <p>메인 애플리케이션 클래스가 아닌 별도 Config 클래스에 선언한 이유:
 * {@code @SpringBootApplication}에 직접 추가하면 슬라이스 테스트(@WebMvcTest 등)
 * 로딩 시 불필요하게 Auditing 컨텍스트가 초기화되어 테스트가 실패할 수 있다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
