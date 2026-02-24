// 사용자 Role
export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN';

// 사용자 타입
export interface User {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  provider?: 'LOCAL' | 'GOOGLE' | 'KAKAO'; // 가입 경로
  createdAt?: string;
}

// 배송지 타입
export interface Address {
  id: number;
  recipientName: string;
  phone: string;
  zipcode: string;
  address: string;
  addressDetail: string;
  isDefault: boolean;
}

// 로그인 요청
export interface LoginRequest {
  email: string;
  password: string;
}

// 회원가입 요청
export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

// 토큰 응답
export interface TokenResponse {
  accessToken: string;
  user: User;
}

// 프로필 수정 요청
export interface ProfileUpdateRequest {
  name: string;
}

// 비밀번호 변경 요청
export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}
