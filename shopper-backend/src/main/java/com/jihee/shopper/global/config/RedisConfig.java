package com.jihee.shopper.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정.
 * Refresh Token 저장에 사용하는 StringRedisTemplate을 Bean으로 등록한다.
 * Spring Boot 자동 구성이 RedisConnectionFactory를 제공하므로 별도 연결 설정은 불필요하다.
 * (application-local.yaml의 spring.data.redis.* 설정 참조)
 */
@Configuration
public class RedisConfig {

    /**
     * String 키/값 전용 RedisTemplate.
     * key:   "RT:{userId}"  → String
     * value: refreshToken   → String
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
