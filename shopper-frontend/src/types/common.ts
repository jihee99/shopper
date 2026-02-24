// API 공통 응답 타입
export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  code?: string; // 에러 코드 (실패 시)
  data: T;
}

// 페이징 응답 타입 (Spring Data JPA Page 구조)
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // 현재 페이지 번호 (0부터 시작)
  first: boolean;
  last: boolean;
  empty: boolean;
}

// API 에러 타입
export interface ApiError {
  success: false;
  code: string;
  message: string;
}

// 페이징 파라미터
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
