import { Link } from 'react-router-dom';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-secondary border-t border-text/10 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* 회사 정보 */}
          <div>
            <h3 className="text-lg font-bold text-text mb-4">Shopper</h3>
            <p className="text-sm text-text/70 mb-2">상호명: (주)쇼퍼</p>
            <p className="text-sm text-text/70 mb-2">대표자: 홍길동</p>
            <p className="text-sm text-text/70 mb-2">
              사업자등록번호: 123-45-67890
            </p>
            <p className="text-sm text-text/70">
              주소: 서울특별시 강남구 테헤란로 123
            </p>
          </div>

          {/* 고객센터 */}
          <div>
            <h3 className="text-lg font-bold text-text mb-4">고객센터</h3>
            <p className="text-sm text-text/70 mb-2">대표번호: 1234-5678</p>
            <p className="text-sm text-text/70 mb-2">
              운영시간: 평일 09:00 - 18:00
            </p>
            <p className="text-sm text-text/70">이메일: support@shopper.com</p>
          </div>

          {/* 링크 */}
          <div>
            <h3 className="text-lg font-bold text-text mb-4">정책</h3>
            <ul className="space-y-2">
              <li>
                <Link
                  to="/terms"
                  className="text-sm text-text/70 hover:text-primary transition-colors"
                >
                  이용약관
                </Link>
              </li>
              <li>
                <Link
                  to="/privacy"
                  className="text-sm text-text/70 hover:text-primary transition-colors"
                >
                  개인정보처리방침
                </Link>
              </li>
              <li>
                <Link
                  to="/refund"
                  className="text-sm text-text/70 hover:text-primary transition-colors"
                >
                  환불정책
                </Link>
              </li>
            </ul>
          </div>
        </div>

        {/* 저작권 */}
        <div className="mt-8 pt-8 border-t border-text/10 text-center">
          <p className="text-sm text-text/70">
            © {currentYear} Shopper. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
