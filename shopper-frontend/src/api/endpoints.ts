import apiClient from './client';
import {
  ApiResponse,
  PageResponse,
  User,
  LoginRequest,
  SignupRequest,
  TokenResponse,
  Product,
  ProductListParams,
  CartResponse,
  AddToCartRequest,
  Order,
  CreateOrderRequest,
  Category,
} from '@/types';

// ========================================
// 인증 API
// ========================================

export const authApi = {
  // 로그인
  login: async (data: LoginRequest) => {
    const response = await apiClient.post<ApiResponse<TokenResponse>>(
      '/auth/login',
      data
    );
    return response.data;
  },

  // 회원가입
  signup: async (data: SignupRequest) => {
    const response = await apiClient.post<ApiResponse<User>>('/auth/signup', data);
    return response.data;
  },

  // 현재 사용자 정보 조회
  getCurrentUser: async () => {
    const response = await apiClient.get<ApiResponse<User>>('/auth/me');
    return response.data;
  },
};

// ========================================
// 상품 API
// ========================================

export const productApi = {
  // 상품 목록 조회 (페이징)
  getProducts: async (params?: ProductListParams) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      '/products',
      { params }
    );
    return response.data;
  },

  // 상품 상세 조회
  getProduct: async (id: number) => {
    const response = await apiClient.get<ApiResponse<Product>>(`/products/${id}`);
    return response.data;
  },

  // 카테고리 목록 조회
  getCategories: async () => {
    const response = await apiClient.get<ApiResponse<Category[]>>('/categories');
    return response.data;
  },

  // 카테고리별 상품 목록 조회
  getProductsByCategory: async (categoryId: number, params?: ProductListParams) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      `/categories/${categoryId}/products`,
      { params }
    );
    return response.data;
  },

  // 상품 검색
  searchProducts: async (keyword: string, params?: ProductListParams) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Product>>>(
      '/products/search',
      { params: { ...params, keyword } }
    );
    return response.data;
  },
};

// ========================================
// 장바구니 API
// ========================================

export const cartApi = {
  // 장바구니 조회
  getCart: async () => {
    const response = await apiClient.get<ApiResponse<CartResponse>>('/cart');
    return response.data;
  },

  // 장바구니에 상품 추가
  addToCart: async (data: AddToCartRequest) => {
    const response = await apiClient.post<ApiResponse<CartResponse>>('/cart', data);
    return response.data;
  },

  // 장바구니 아이템 수량 변경
  updateCartItem: async (cartItemId: number, quantity: number) => {
    const response = await apiClient.patch<ApiResponse<CartResponse>>(
      `/cart/items/${cartItemId}`,
      { quantity }
    );
    return response.data;
  },

  // 장바구니 아이템 삭제
  deleteCartItem: async (cartItemId: number) => {
    const response = await apiClient.delete<ApiResponse<void>>(
      `/cart/items/${cartItemId}`
    );
    return response.data;
  },

  // 장바구니 전체 비우기
  clearCart: async () => {
    const response = await apiClient.delete<ApiResponse<void>>('/cart');
    return response.data;
  },
};

// ========================================
// 주문 API
// ========================================

export const orderApi = {
  // 주문 생성
  createOrder: async (data: CreateOrderRequest) => {
    const response = await apiClient.post<ApiResponse<Order>>('/orders', data);
    return response.data;
  },

  // 내 주문 목록 조회
  getMyOrders: async (page?: number, size?: number) => {
    const response = await apiClient.get<ApiResponse<PageResponse<Order>>>(
      '/orders/me',
      { params: { page, size } }
    );
    return response.data;
  },

  // 주문 상세 조회
  getOrder: async (orderId: number) => {
    const response = await apiClient.get<ApiResponse<Order>>(`/orders/${orderId}`);
    return response.data;
  },

  // 주문 취소
  cancelOrder: async (orderId: number) => {
    const response = await apiClient.post<ApiResponse<Order>>(
      `/orders/${orderId}/cancel`
    );
    return response.data;
  },
};

// ========================================
// 사용자 API
// ========================================

export const userApi = {
  // 내 정보 조회
  getMyProfile: async () => {
    const response = await apiClient.get<ApiResponse<User>>('/users/me');
    return response.data;
  },

  // 내 정보 수정
  updateMyProfile: async (name: string) => {
    const response = await apiClient.patch<ApiResponse<User>>('/users/me', { name });
    return response.data;
  },

  // 비밀번호 변경
  changePassword: async (currentPassword: string, newPassword: string) => {
    const response = await apiClient.patch<ApiResponse<void>>(
      '/users/me/password',
      { currentPassword, newPassword }
    );
    return response.data;
  },
};
