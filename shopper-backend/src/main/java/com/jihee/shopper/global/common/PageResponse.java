package com.jihee.shopper.global.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 래퍼 클래스.
 *
 * <p>Spring Data의 {@link Page}를 받아 프론트엔드 친화적인 구조로 변환한다.
 *
 * <pre>
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 100,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false
 * }
 * </pre>
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;           // 현재 페이지 (0-based)
    private final int size;           // 페이지 크기
    private final long totalElements; // 전체 데이터 수
    private final int totalPages;     // 전체 페이지 수
    private final boolean first;      // 첫 번째 페이지 여부
    private final boolean last;       // 마지막 페이지 여부

    private PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
}
