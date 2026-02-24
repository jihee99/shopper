import { Product } from './product';

// 장바구니 아이템
export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
  createdAt: string;
}

// 장바구니 응답
export interface CartResponse {
  items: CartItem[];
  totalPrice: number;
  totalCount: number;
}

// 장바구니 추가 요청
export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

// 장바구니 수량 변경 요청
export interface UpdateCartItemRequest {
  quantity: number;
}
