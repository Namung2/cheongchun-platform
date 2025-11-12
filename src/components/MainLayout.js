// 📁 src/components/MainLayout.js
import { Link } from "react-router-dom";
import BottomNav from "./BottomNav";

function MainLayout({ children }) {
  return (
    <div className="sb-nav-fixed d-flex flex-column min-vh-100">
      <nav className="navbar navbar-light bg-white w-100 fixed-top border-bottom">
  <div className="container-fluid d-flex justify-content-between align-items-center px-3">
    <Link 
      to="/" 
      className="navbar-brand fw-bold"
      style={{ color: '#16df63ff' }}   
    >
      하루안부
    </Link>
        </div>
      </nav>

      {/* 메인 콘텐츠 */}
      <main className="flex-fill bg-white">
        {children}
      </main>

      {/* 하단 네비게이션 */}
      <BottomNav />
    </div>
  );
}

export default MainLayout;