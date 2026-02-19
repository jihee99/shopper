package com.jihee.shopper.domain.user.entity;

/**
 * 사용자 권한 역할 (ADR-02-005: JWT role 클레임 값으로도 사용).
 * Spring Security의 hasRole("USER") 는 "ROLE_USER" 권한을 확인하므로
 * enum 이름 자체를 "ROLE_USER" 형식으로 정의한다.
 */
public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN
}
