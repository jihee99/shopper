// 카테고리 타입
export interface Category {
  id: number;
  name: string;
  description?: string;
}

// 상품 이미지 타입
export interface ProductImage {
  id: number;
  imageUrl: string;
  displayOrder: number;
}

// 상품 타입
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  category: Category;
  images: ProductImage[];
  createdAt: string;
  updatedAt: string;
}

// 상품 목록 조회 파라미터
export interface ProductListParams {
  page?: number;
  size?: number;
  categoryId?: number;
  keyword?: string;
  sort?: string;
}

// 상품 등록/수정 요청
export interface ProductFormData {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  images?: File[]; // 이미지 파일 업로드
}
