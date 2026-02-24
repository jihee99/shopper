import { Product } from './product';
import { Address } from './user';

// 주문 상태
export type OrderStatus =
  | 'PENDING'      // 결제 대기
  | 'PAID'         // 결제 완료
  | 'PREPARING'    // 배송 준비
  | 'SHIPPED'      // 배송 중
  | 'DELIVERED'    // 배송 완료
  | 'CANCELLED'    // 취소
  | 'REFUNDED';    // 환불

// 주문 아이템
export interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  price: number; // 주문 당시 가격
}

// 주문
export interface Order {
  id: number;
  orderNumber: string;
  items: OrderItem[];
  address: Address;
  totalPrice: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

// 주문 생성 요청
export interface CreateOrderRequest {
  addressId: number;
  items: {
    productId: number;
    quantity: number;
  }[];
}

// 결제 정보
export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  method: 'CARD' | 'VIRTUAL_ACCOUNT' | 'TRANSFER';
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  paymentKey?: string; // 토스페이먼츠 결제 키
  createdAt: string;
}

// 토스페이먼츠 결제 승인 요청
export interface PaymentApprovalRequest {
  orderId: string;
  paymentKey: string;
  amount: number;
}
