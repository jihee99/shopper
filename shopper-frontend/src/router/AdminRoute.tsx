import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/useAuthStore';

/**
 * AdminRoute: 관리자 권한이 있는 사용자만 접근 가능한 라우트
 * 비인증 사용자는 /login으로, 일반 사용자는 /로 리다이렉트
 */
export const AdminRoute = () => {
  const { isAuthenticated, isAdmin } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    // 로그인했지만 관리자가 아닌 경우
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};
