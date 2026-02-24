import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuthStore } from '@/store/useAuthStore';
import { useCartStore } from '@/store/useCartStore';

const Header = () => {
  const navigate = useNavigate();
  const [searchKeyword, setSearchKeyword] = useState('');
  const { isAuthenticated, isAdmin, user, logout } = useAuthStore();
  const itemCount = useCartStore((state) => state.itemCount);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchKeyword.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchKeyword.trim())}`);
      setSearchKeyword('');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="bg-background border-b border-secondary sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* 로고 */}
          <Link to="/" className="flex items-center space-x-2">
            <div className="text-2xl font-bold text-primary">Shopper</div>
          </Link>

          {/* 검색창 */}
          <form onSubmit={handleSearch} className="flex-1 max-w-md mx-8">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="상품을 검색하세요"
              className="w-full px-4 py-2 border border-secondary rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </form>

          {/* 우측 메뉴 */}
          <div className="flex items-center space-x-6">
            {/* 장바구니 */}
            <Link
              to="/cart"
              className="relative text-text hover:text-primary transition-colors"
            >
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                />
              </svg>
              {itemCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-accent text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {itemCount}
                </span>
              )}
            </Link>

            {/* 로그인/유저 메뉴 */}
            {!isAuthenticated ? (
              <Link
                to="/login"
                className="text-text hover:text-primary font-medium transition-colors"
              >
                로그인
              </Link>
            ) : (
              <div className="relative group">
                <button className="text-text hover:text-primary font-medium transition-colors">
                  {user?.name || '사용자'}
                </button>
                {/* 드롭다운 메뉴 */}
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg py-2 invisible group-hover:visible opacity-0 group-hover:opacity-100 transition-all">
                  <Link
                    to="/mypage"
                    className="block px-4 py-2 text-sm text-text hover:bg-secondary"
                  >
                    마이페이지
                  </Link>
                  {isAdmin && (
                    <Link
                      to="/admin/products"
                      className="block px-4 py-2 text-sm text-accent hover:bg-secondary"
                    >
                      관리자 페이지
                    </Link>
                  )}
                  <button
                    onClick={handleLogout}
                    className="block w-full text-left px-4 py-2 text-sm text-text hover:bg-secondary"
                  >
                    로그아웃
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Header;
