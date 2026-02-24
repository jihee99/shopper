import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';

/**
 * PrivateRoute: 로그인한 사용자만 접근 가능한 라우트
 * 비인증 사용자는 /login으로 리다이렉트
 */
export const PrivateRoute = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};
