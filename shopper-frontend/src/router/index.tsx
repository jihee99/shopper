import { createBrowserRouter } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import { PrivateRoute } from './PrivateRoute';
import { AdminRoute } from './AdminRoute';

// Placeholder 페이지 컴포넌트들 (Phase별로 구현 예정)
const HomePage = () => <div className="p-8">Home Page</div>;
const LoginPage = () => <div className="p-8">Login Page</div>;
const SignupPage = () => <div className="p-8">Signup Page</div>;
const ProductListPage = () => <div className="p-8">Product List Page</div>;
const ProductDetailPage = () => <div className="p-8">Product Detail Page</div>;
const CategoryProductPage = () => <div className="p-8">Category Product Page</div>;
const SearchResultPage = () => <div className="p-8">Search Result Page</div>;
const CartPage = () => <div className="p-8">Cart Page</div>;
const OrderPage = () => <div className="p-8">Order Page</div>;
const OrderSuccessPage = () => <div className="p-8">Order Success Page</div>;
const OrderFailPage = () => <div className="p-8">Order Fail Page</div>;
const MyPage = () => <div className="p-8">My Page</div>;
const ProfilePage = () => <div className="p-8">Profile Page</div>;
const AddressPage = () => <div className="p-8">Address Page</div>;
const OrderHistoryPage = () => <div className="p-8">Order History Page</div>;
const OrderDetailPage = () => <div className="p-8">Order Detail Page</div>;
const AdminProductListPage = () => <div className="p-8">Admin Product List</div>;
const AdminProductEditPage = () => <div className="p-8">Admin Product Edit</div>;
const AdminOrderListPage = () => <div className="p-8">Admin Order List</div>;
const AdminOrderDetailPage = () => <div className="p-8">Admin Order Detail</div>;
const AdminUserListPage = () => <div className="p-8">Admin User List</div>;
const AdminUserDetailPage = () => <div className="p-8">Admin User Detail</div>;
const NotFoundPage = () => <div className="p-8">404 Not Found</div>;
const OAuthCallbackPage = () => <div className="p-8">OAuth Callback</div>;

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      // 공개 화면
      { index: true, element: <HomePage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:id', element: <ProductDetailPage /> },
      { path: 'categories/:id', element: <CategoryProductPage /> },
      { path: 'search', element: <SearchResultPage /> },

      // 인증 화면
      { path: 'login', element: <LoginPage /> },
      { path: 'signup', element: <SignupPage /> },
      { path: 'oauth/callback', element: <OAuthCallbackPage /> },

      // PrivateRoute 보호 그룹 (로그인 필요)
      {
        element: <PrivateRoute />,
        children: [
          // 장바구니
          { path: 'cart', element: <CartPage /> },

          // 주문/결제
          { path: 'order', element: <OrderPage /> },
          { path: 'order/success', element: <OrderSuccessPage /> },
          { path: 'order/fail', element: <OrderFailPage /> },

          // 마이페이지
          { path: 'mypage', element: <MyPage /> },
          { path: 'mypage/profile', element: <ProfilePage /> },
          { path: 'mypage/addresses', element: <AddressPage /> },
          { path: 'mypage/orders', element: <OrderHistoryPage /> },
          { path: 'mypage/orders/:id', element: <OrderDetailPage /> },
        ],
      },

      // AdminRoute 보호 그룹 (관리자 권한 필요)
      {
        path: 'admin',
        element: <AdminRoute />,
        children: [
          // 상품 관리
          { path: 'products', element: <AdminProductListPage /> },
          { path: 'products/new', element: <AdminProductEditPage /> },
          { path: 'products/:id/edit', element: <AdminProductEditPage /> },

          // 주문 관리
          { path: 'orders', element: <AdminOrderListPage /> },
          { path: 'orders/:id', element: <AdminOrderDetailPage /> },

          // 회원 관리
          { path: 'users', element: <AdminUserListPage /> },
          { path: 'users/:id', element: <AdminUserDetailPage /> },
        ],
      },

      // 404
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
